/* Modelo: estado de la sesión de trabajo del estudiante.
   Las vistas escuchan los eventos "lesson" y "build". */

export class WorkspaceModel extends EventTarget {
  lesson = null; // { id, name, objective, html }
  attempt = null; // { id, completed }
  build = { state: "idle", code: null }; // idle | busy | ok | error | stale
  session = { authenticated: false };

  setLesson(lesson, attempt) {
    this.lesson = lesson;
    this.attempt = attempt ? { id: attempt.id, completed: false } : null;
    this.#emit("lesson");
  }

  clearLesson() {
    this.lesson = null;
    this.attempt = null;
    this.#emit("lesson");
  }

  markCompleted() {
    if (!this.attempt || this.attempt.completed) return false;
    this.attempt.completed = true;
    this.#emit("lesson");
    return true;
  }

  setBuild(state, code = this.build.code) {
    this.build = { state, code };
    this.#emit("build");
  }

  /** Ejecutar solo se permite si la última compilación fue exitosa y el código no cambió. */
  canRun(currentCode) {
    return this.build.state === "ok" && this.build.code === currentCode;
  }

  #emit(type) {
    this.dispatchEvent(new Event(type));
  }
}
