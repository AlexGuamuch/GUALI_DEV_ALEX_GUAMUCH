/* Vista: título de lección activa y panel de lección. */

import { $$, h, icon } from "../../shared/dom.js";
import { OBJECTIVES } from "../../shared/format.js";

export class LessonView {
  constructor({ title, meta, body }) {
    this.title = title;
    this.meta = meta;
    this.body = body;
    this.emptyState = body.firstElementChild.cloneNode(true);
  }

  render(model) {
    const { lesson, attempt } = model;

    for (const el of $$("[data-needs-lesson]")) el.disabled = !lesson;

    if (!lesson) {
      this.title.textContent = "Ninguna lección abierta";
      this.title.classList.add("is-empty");
      this.meta.replaceChildren();
      this.body.replaceChildren(this.emptyState.cloneNode(true));
      delete this.body.dataset.lessonId;
      return;
    }

    const objective = OBJECTIVES[lesson.objective];
    this.title.textContent = lesson.name;
    this.title.classList.remove("is-empty");
    this.meta.replaceChildren(
      h("span", { class: "badge no-dot" }, `Objetivo · ${objective.label}`),
      attempt?.completed
        ? h("span", { class: "badge badge-success" }, "Completada")
        : h("span", { class: "badge badge-warning" }, "En curso"),
    );

    if (this.body.dataset.lessonId === String(lesson.id)) return;
    this.body.dataset.lessonId = String(lesson.id);

    // El HTML de la lección llega ya sanitizado desde el servidor (markdown → HTML seguro).
    const content = document.createElement("template");
    content.innerHTML = lesson.html;

    this.body.replaceChildren(
      h("div", { class: "lesson-scroll" },
        h("div", { class: "objective-card" },
          icon("target"),
          h("div", {},
            h("strong", {}, `Objetivo: ${objective.label}`),
            h("span", {}, objective.description))),
        h("article", { class: "prose" }, content.content)),
    );
    this.body.scrollTop = 0;
  }
}
