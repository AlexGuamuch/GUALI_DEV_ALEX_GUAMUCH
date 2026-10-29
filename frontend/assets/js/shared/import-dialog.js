/* Diálogo "Importar compendio" (.zip). Lo usan la interfaz principal y el panel de administración.
   Después de importar muestra qué lecciones se crearon, cuáles se actualizaron y cuáles se rechazaron y por qué. */

import { $, h, icon } from "./dom.js";
import { formatBytes } from "./format.js";

const MAX_BYTES = 10 * 1024 * 1024;

export class ImportDialog {
  #file = null;
  #terminado = false;

  constructor(api, { onImported = () => {}, notify }) {
    this.api = api;
    this.onImported = onImported;
    this.notify = notify;
    this.dialog = $("#dlg-import");
    this.input = $("#bundle-file");
    this.dropzone = $("#dropzone");
    this.button = $("#btn-import");
    this.picked = $("#bundle-picked");
    this.tree = $(".tree", this.dialog);
    this.result = $("#import-result");
    this.cancel = $("[data-close].btn", this.dialog);

    this.input.addEventListener("change", () => this.#pick(this.input.files[0]));
    this.dropzone.addEventListener("dragover", (event) => {
      event.preventDefault();
      this.dropzone.classList.add("is-over");
    });
    this.dropzone.addEventListener("dragleave", () => this.dropzone.classList.remove("is-over"));
    this.dropzone.addEventListener("drop", (event) => {
      event.preventDefault();
      this.dropzone.classList.remove("is-over");
      this.#pick(event.dataTransfer.files[0]);
    });
    this.button.addEventListener("click", () => (this.#terminado ? this.dialog.close() : this.#import()));
  }

  show() {
    this.#file = null;
    this.#terminado = false;
    this.input.value = "";
    this.dropzone.hidden = false;
    this.tree.hidden = false;
    this.picked.hidden = true;
    this.result.hidden = true;
    this.result.replaceChildren();
    this.cancel.hidden = false;
    this.#setButton("upload-simple", "Importar", true);
    this.dialog.showModal();
  }

  #pick(file) {
    if (!file) return;
    if (!file.name.toLowerCase().endsWith(".zip")) {
      this.notify("El compendio debe ser un archivo .zip.", "danger");
      return;
    }
    if (file.size > MAX_BYTES) {
      this.notify("El archivo supera el máximo de 10 MB.", "danger");
      return;
    }
    this.#file = file;
    $("#bundle-name").textContent = file.name;
    $("#bundle-size").textContent = formatBytes(file.size);
    this.picked.hidden = false;
    this.button.disabled = false;
  }

  async #import() {
    if (!this.#file) return;
    this.#setButton("circle-notch", "Importando…", true, true);
    try {
      const r = await this.api.importBundle(this.#file);
      this.#mostrar(r);
      this.onImported(r);
    } catch (error) {
      this.notify(error.message, "danger");
      this.#setButton("upload-simple", "Importar", false);
    }
  }

  #mostrar(r) {
    this.#terminado = true;
    this.dropzone.hidden = true;
    this.tree.hidden = true;
    this.picked.hidden = true;
    this.cancel.hidden = true;
    this.#setButton("check", "Listo", false);

    const total = r.created.length + r.updated.length;
    const partes = [
      h("div", { class: "import-summary" },
        resumen("Nuevas", r.created.length),
        resumen("Actualizadas", r.updated.length),
        resumen("Rechazadas", r.rejected.length)),
      total === 0 && r.rejected.length === 0
        ? h("p", { class: "muted" }, "El compendio no contenía lecciones.")
        : null,
      seccion("Lecciones nuevas", r.created.map((n) => fila(n, null, "plus-circle", "success"))),
      seccion("Lecciones actualizadas (se reemplazaron por la nueva versión)",
        r.updated.map((n) => fila(n, null, "arrows-clockwise", "info"))),
      seccion("Rechazadas", r.rejected.map((x) => fila(x.lesson, x.reason, "x-circle", "danger"))),
      seccion("Advertencias", r.warnings.map((w) => fila(w, null, "info", "muted"))),
    ];
    // replaceChildren convertiría null en el texto "null": se descartan las secciones vacías.
    this.result.replaceChildren(...partes.filter(Boolean));
    this.result.hidden = false;
  }

  #setButton(iconName, label, disabled, spin = false) {
    this.button.replaceChildren(
      h("i", { class: `ph-bold ph-${iconName}${spin ? " spin" : ""}`, "aria-hidden": "true" }),
      label);
    this.button.disabled = disabled;
  }
}

function resumen(label, value) {
  return h("div", { class: "stat-mini" }, h("span", { class: "eyebrow" }, label), h("strong", {}, String(value)));
}

function seccion(titulo, filas) {
  if (!filas.length) return null;
  return h("section", { class: "import-section" },
    h("h3", { class: "eyebrow" }, titulo),
    h("ul", { class: "import-list" }, filas));
}

function fila(nombre, motivo, iconName, tono) {
  return h("li", { class: `is-${tono}` },
    icon(iconName, "fill"),
    h("span", {},
      h("span", { class: "import-name" }, nombre),
      motivo ? h("span", { class: "import-reason" }, motivo) : null));
}
