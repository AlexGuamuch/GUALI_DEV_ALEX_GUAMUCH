/* Controlador: coordina el modelo, las vistas y la API. */

import { $, $$ } from "../../shared/dom.js";
import { sourceFileName } from "../../shared/format.js";
import { toast } from "../../shared/toast.js";

const BUILD_TEXT = {
  idle: "Sin compilar",
  busy: "Trabajando…",
  ok: "Compilado sin errores",
  error: "Con errores de compilación",
  stale: "Código modificado: vuelve a compilar",
};

export class WorkspaceController {
  #busy = false;
  #stateBeforeEdit = "ok";

  constructor({ api, model, lessonView, editor, consoleView, dialogs }) {
    this.api = api;
    this.model = model;
    this.lessonView = lessonView;
    this.editor = editor;
    this.console = consoleView;
    this.dialogs = dialogs;

    this.compileButton = $("#btn-compile");
    this.runButton = $("#btn-run");
    this.buildState = $("#build-state");
    this.buildText = $("#build-state-text");
    this.workspace = $("#workspace");
  }

  async init() {
    this.model.addEventListener("lesson", () => this.lessonView.render(this.model));
    this.model.addEventListener("build", () => this.#renderBuild());

    this.editor.addEventListener("change", () => this.#onCodeChanged());
    this.editor.addEventListener("blocked-paste", () =>
      toast("Pegar está deshabilitado: escribe el código a mano para practicar la sintaxis.", { type: "warning" }));

    this.compileButton.addEventListener("click", () => this.compile());
    this.runButton.addEventListener("click", () => this.run());
    $("#btn-clear-console").addEventListener("click", () => this.console.clear());

    document.addEventListener("click", (event) => this.#onAction(event));
    document.addEventListener("keydown", (event) => this.#onShortcut(event));

    for (const tab of $$(".mobile-tabs [data-view]")) {
      tab.addEventListener("click", () => this.#showView(tab.dataset.view));
    }

    // Cerrar la pestaña con una lección sin completar la registra como abandonada (Pendiente).
    window.addEventListener("pagehide", () => {
      const { attempt } = this.model;
      if (attempt && !attempt.completed) this.api.finishAttempt(attempt.id, { keepalive: true });
    });

    this.model.session = await this.api.getSession();
    this.#renderSession();
    this.lessonView.render(this.model);
    this.#renderBuild();
  }

  /* ---------- Acciones de menú ---------- */

  #onAction(event) {
    const target = event.target.closest("[data-action]");
    if (!target || target.disabled) return;

    switch (target.dataset.action) {
      case "open-lesson":
        this.dialogs.openLesson.show();
        break;
      case "close-lesson":
        this.closeLesson();
        break;
      case "show-history":
        this.dialogs.history.show();
        break;
      case "help":
        this.dialogs.help.show(target.dataset.helpTab);
        break;
      case "import-bundle":
        this.dialogs.importBundle.show();
        break;
      case "export-lessons":
        this.#exportLessons();
        break;
      case "delete-lesson":
        this.dialogs.deleteLesson.show();
        break;
      case "logout":
        this.#logout();
        break;
      default:
        break;
    }
  }

  #onShortcut(event) {
    const mod = event.ctrlKey || event.metaKey;
    if (!mod) return;
    if (event.key === "Enter" && event.shiftKey) {
      event.preventDefault();
      this.run();
    } else if (event.key === "Enter") {
      event.preventDefault();
      this.compile();
    } else if (event.key.toLowerCase() === "o" && !document.querySelector("dialog[open]")) {
      event.preventDefault();
      this.dialogs.openLesson.show();
    }
  }

  /* ---------- Lecciones ---------- */

  async openLesson(id) {
    try {
      await this.#finishCurrentAttempt();
      const lesson = await this.api.getLesson(id);
      const attempt = await this.api.startAttempt(lesson);
      this.model.setLesson(lesson, attempt);
      this.editor.value = "";
      this.console.clear();
      this.model.setBuild("idle", null);
      this.#showView("lesson");
      toast(`Lección abierta: ${lesson.name}`, { type: "success", timeout: 2600 });
    } catch (error) {
      toast(error.message, { type: "danger" });
    }
  }

  async closeLesson() {
    if (!this.model.lesson) return;
    const name = this.model.lesson.name;
    await this.#finishCurrentAttempt();
    this.model.clearLesson();
    this.editor.value = "";
    this.console.clear();
    this.model.setBuild("idle", null);
    toast(`Lección cerrada: ${name}`, { type: "info", timeout: 2600 });
  }

  async #finishCurrentAttempt() {
    const { attempt } = this.model;
    if (attempt && !attempt.completed) {
      await this.api.finishAttempt(attempt.id);
    }
  }

  async #exportLessons() {
    try {
      const { fileName } = await this.api.exportLessons();
      toast(`Descarga lista: ${fileName}`, { type: "success" });
    } catch (error) {
      toast(error.message, { type: "danger" });
    }
  }

  /* ---------- Compilar y ejecutar ---------- */

  async compile() {
    if (this.#busy) return;
    const code = this.editor.value;
    this.#setBusy(true, this.compileButton);
    this.model.setBuild("busy", code);
    this.console.print(`$ javac ${sourceFileName(code)}`, "meta");

    try {
      const result = await this.api.compile(code, this.model.attempt?.id);
      if (result.success) {
        this.console.print(`Compilación exitosa · 0 errores · ${seconds(result.durationMs)}`, "success");
        this.model.setBuild("ok", code);
      } else {
        this.console.print(result.output, "error");
        this.console.print(`La compilación falló · ${seconds(result.durationMs)}`, "meta");
        this.model.setBuild("error", code);
      }
      this.#objectiveResult(result.objectiveMet);
    } catch (error) {
      this.console.print(`No se pudo compilar: ${error.message}`, "error");
      this.model.setBuild("idle", null);
    } finally {
      this.#setBusy(false, this.compileButton);
    }
  }

  async run() {
    const code = this.editor.value;
    if (this.#busy || !this.model.canRun(code)) return;
    this.#setBusy(true, this.runButton);
    this.console.print(`$ java ${sourceFileName(code).replace(".java", "")}`, "meta");

    try {
      const result = await this.api.execute(code, this.model.attempt?.id);
      if (result.compileFailed) {
        this.console.print(result.compileOutput, "error");
        this.model.setBuild("error", code);
      } else if (result.timeout) {
        this.console.print("El programa se detuvo porque superó el tiempo máximo de 5 segundos.", "warning");
      } else {
        if (result.stdout) this.console.print(result.stdout);
        if (result.stderr) this.console.print(result.stderr, "error");
        this.console.print(
          result.success
            ? `Programa terminado sin errores · ${seconds(result.durationMs)}`
            : `El programa terminó con un error de ejecución · ${seconds(result.durationMs)}`,
          "meta");
      }
      this.#objectiveResult(result.objectiveMet);
    } catch (error) {
      this.console.print(`No se pudo ejecutar: ${error.message}`, "error");
    } finally {
      this.#setBusy(false, this.runButton);
    }
  }

  /** El servidor evalúa el objetivo de la lección después de cada acción y avisa si se cumplió. */
  #objectiveResult(met) {
    if (met && this.model.markCompleted()) {
      this.console.goal("Objetivo cumplido: la lección quedó marcada como completada en tu historial.");
      toast("¡Objetivo cumplido! Lección completada.", { type: "success" });
    }
  }

  #onCodeChanged() {
    const { state, code } = this.model.build;
    if ((state === "ok" || state === "error") && code !== this.editor.value) {
      this.#stateBeforeEdit = state;
      this.model.setBuild("stale", code);
    } else if (state === "stale" && code === this.editor.value) {
      this.model.setBuild(this.#stateBeforeEdit, code);
    }
  }

  #renderBuild() {
    const { state } = this.model.build;
    this.buildState.dataset.state = state;
    this.buildText.textContent = BUILD_TEXT[state];
    this.runButton.disabled = this.#busy || !this.model.canRun(this.editor.value);
  }

  #setBusy(busy, button) {
    this.#busy = busy;
    const iconEl = button.querySelector("i");
    if (busy) {
      button.dataset.icon = iconEl.className;
      iconEl.className = "ph-bold ph-circle-notch spin";
    } else if (button.dataset.icon) {
      iconEl.className = button.dataset.icon;
    }
    this.compileButton.disabled = busy;
    this.#renderBuild();
  }

  /* ---------- Sesión y vista ---------- */

  #renderSession() {
    const admin = this.model.session.authenticated;
    for (const el of $$("[data-admin-only]")) el.hidden = !admin;
    for (const el of $$("[data-student-only]")) el.hidden = admin;
    if (admin) $("#admin-username").textContent = this.model.session.username;
  }

  async #logout() {
    await this.api.logout();
    this.model.session = { authenticated: false };
    this.#renderSession();
    toast("Sesión de administrador cerrada.", { type: "info" });
  }

  #showView(view) {
    this.workspace.dataset.view = view;
    for (const tab of $$(".mobile-tabs [data-view]")) {
      tab.setAttribute("aria-selected", String(tab.dataset.view === view));
    }
  }
}

function seconds(ms) {
  return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;
}
