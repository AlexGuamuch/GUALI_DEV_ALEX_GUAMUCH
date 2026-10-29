/* Vistas de diálogos: abrir lección, historial, ayuda y eliminar lección (admin). */

import { $, $$, h, icon } from "../../shared/dom.js";
import { OBJECTIVES, formatDateTime } from "../../shared/format.js";

/** Cierre por botones [data-close] y por clic en el fondo. */
export function initDialogs() {
  for (const dialog of $$("dialog.dialog")) {
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog || event.target.closest("[data-close]")) dialog.close();
    });
  }
}

/* ---------- Abrir lección ---------- */

export class OpenLessonDialog {
  #lessons = [];
  #completed = new Set();
  #selected = null;

  constructor(api, onOpen) {
    this.api = api;
    this.onOpen = onOpen;
    this.dialog = $("#dlg-open-lesson");
    this.list = $("#lesson-list");
    this.search = $("#lesson-search");
    this.button = $("#btn-open-selected");
    this.count = $("#lesson-count");

    this.search.addEventListener("input", () => this.#render());
    this.button.addEventListener("click", () => this.#confirm());
    this.dialog.addEventListener("keydown", (event) => this.#onKeyDown(event));
  }

  async show() {
    this.#selected = null;
    this.search.value = "";
    this.button.disabled = true;
    this.list.replaceChildren(h("li", { class: "muted" }, "Cargando lecciones…"));
    this.count.textContent = "";
    this.dialog.showModal();

    const [lessons, history] = await Promise.all([this.api.listLessons(), this.api.listHistory()]);
    this.#lessons = lessons;
    this.#completed = new Set(history.filter((a) => a.status === "COMPLETADA").map((a) => a.lessonName));
    this.#render();
    this.search.focus();
  }

  #visible() {
    const q = this.search.value.trim().toLowerCase();
    return this.#lessons.filter((l) => !q || l.name.toLowerCase().includes(q) || l.summary.toLowerCase().includes(q));
  }

  #render() {
    const lessons = this.#visible();
    this.count.textContent = `${lessons.length} de ${this.#lessons.length} lecciones`;

    if (!lessons.length) {
      this.list.replaceChildren(h("li", { class: "table-empty" }, "No hay lecciones que coincidan con la búsqueda."));
      return;
    }

    this.list.replaceChildren(...lessons.map((lesson, i) => {
      const objective = OBJECTIVES[lesson.objective];
      const option = h("button", {
        class: "lesson-option",
        type: "button",
        role: "option",
        "aria-selected": String(this.#selected === lesson.id),
        onclick: () => this.#select(lesson.id),
        ondblclick: () => this.#confirm(lesson.id),
      },
      h("span", { class: "lesson-option-index" }, String(i + 1).padStart(2, "0")),
      h("span", { class: "lesson-option-main" },
        h("span", { class: "lesson-option-name" }, lesson.name),
        h("span", { class: "lesson-option-desc" }, lesson.summary)),
      h("span", { class: `badge no-dot ${objective.badge}` }, objective.label),
      this.#completed.has(lesson.name)
        ? h("span", { class: "lesson-option-done", title: "Ya la completaste antes" }, icon("check-circle", "fill"))
        : null);
      option.dataset.id = String(lesson.id);
      option.style.setProperty("--i", String(i));
      return h("li", {}, option);
    }));
  }

  #select(id) {
    this.#selected = id;
    this.button.disabled = false;
    for (const el of $$(".lesson-option", this.list)) {
      el.setAttribute("aria-selected", String(el.dataset.id === String(id)));
    }
  }

  #confirm(id = this.#selected) {
    if (id == null) return;
    this.dialog.close();
    this.onOpen(id);
  }

  #onKeyDown(event) {
    const lessons = this.#visible();
    if (!lessons.length) return;
    const index = lessons.findIndex((l) => l.id === this.#selected);
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const next = event.key === "ArrowDown" ? Math.min(lessons.length - 1, index + 1) : Math.max(0, index - 1);
      this.#select(lessons[next].id);
      $(`.lesson-option[data-id="${lessons[next].id}"]`, this.list)?.scrollIntoView({ block: "nearest" });
    } else if (event.key === "Enter" && this.#selected != null) {
      event.preventDefault();
      this.#confirm();
    }
  }
}

/* ---------- Historial de sesión ---------- */

export class HistoryDialog {
  constructor(api) {
    this.api = api;
    this.dialog = $("#dlg-history");
    this.rows = $("#history-rows");
    this.summary = $("#history-summary");
  }

  async show() {
    this.rows.replaceChildren(h("tr", {}, h("td", { colspan: "4", class: "table-empty" }, "Cargando historial…")));
    this.summary.replaceChildren();
    this.dialog.showModal();

    const attempts = await this.api.listHistory();
    const completed = attempts.filter((a) => a.status === "COMPLETADA").length;

    this.summary.replaceChildren(
      stat("Intentos", attempts.length),
      stat("Completadas", completed),
      stat("Pendientes", attempts.length - completed),
    );

    if (!attempts.length) {
      this.rows.replaceChildren(h("tr", {}, h("td", { colspan: "4", class: "table-empty" }, "Aún no has abierto ninguna lección.")));
      return;
    }

    this.rows.replaceChildren(...attempts.map((a) => h("tr", {},
      h("td", { class: "cell-mono" }, a.lessonName),
      h("td", { class: "cell-mono" }, formatDateTime(a.startedAt)),
      h("td", { class: "cell-mono" }, a.endedAt ? formatDateTime(a.endedAt) : "—"),
      h("td", {}, statusBadge(a)))));
  }
}

function stat(label, value) {
  return h("div", { class: "stat-mini" }, h("span", { class: "eyebrow" }, label), h("strong", {}, String(value)));
}

function statusBadge(attempt) {
  if (attempt.status === "COMPLETADA") return h("span", { class: "badge badge-success" }, "Completada");
  if (!attempt.endedAt) return h("span", { class: "badge badge-warning" }, "En curso");
  return h("span", { class: "badge badge-danger" }, "Pendiente");
}

/* ---------- Ayuda ---------- */

export class HelpDialog {
  constructor() {
    this.dialog = $("#dlg-help");
    for (const tab of $$("[role=tab][data-help-tab]", this.dialog)) {
      tab.addEventListener("click", () => this.#activate(tab.dataset.helpTab));
    }
  }

  show(tab = "guia") {
    this.#activate(tab);
    this.dialog.showModal();
  }

  #activate(name) {
    for (const tab of $$("[role=tab][data-help-tab]", this.dialog)) {
      tab.setAttribute("aria-selected", String(tab.dataset.helpTab === name));
    }
    for (const panel of $$("[data-help-panel]", this.dialog)) {
      panel.hidden = panel.dataset.helpPanel !== name;
    }
  }
}

/* ---------- Eliminar lección (admin) ---------- */

export class DeleteLessonDialog {
  constructor(api, { onDeleted, notify }) {
    this.api = api;
    this.onDeleted = onDeleted;
    this.notify = notify;
    this.dialog = $("#dlg-delete");
    this.select = $("#delete-lesson-select");
    this.confirm = $("#delete-confirm");
    this.button = $("#btn-delete");

    this.confirm.addEventListener("change", () => {
      this.button.disabled = !this.confirm.checked || !this.select.value;
    });
    this.button.addEventListener("click", () => this.#delete());
  }

  async show() {
    this.confirm.checked = false;
    this.button.disabled = true;
    const lessons = await this.api.listLessons();
    this.select.replaceChildren(...lessons.map((l) => h("option", { value: String(l.id) }, l.name)));
    this.dialog.showModal();
  }

  async #delete() {
    const id = Number(this.select.value);
    const name = this.select.selectedOptions[0]?.textContent;
    this.button.disabled = true;
    try {
      await this.api.deleteLesson(id);
      this.dialog.close();
      this.onDeleted(id, name);
    } catch (error) {
      this.notify(error.message, "danger");
      this.button.disabled = false;
    }
  }
}
