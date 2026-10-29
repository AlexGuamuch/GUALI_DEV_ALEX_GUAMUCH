/* Vista: editor de código. Textarea con números de línea, sangría automática
   y bloqueo de pegado (el estudiante debe escribir el código a mano). */

import { sourceFileName } from "../../shared/format.js";

const INDENT = "    ";

export class EditorView extends EventTarget {
  #lineCount = 0;
  #escapePressed = false;

  constructor({ textarea, gutter, cursorLabel, countLabel, fileLabel }) {
    super();
    this.textarea = textarea;
    this.gutter = gutter;
    this.cursorLabel = cursorLabel;
    this.countLabel = countLabel;
    this.fileLabel = fileLabel;

    textarea.addEventListener("input", () => {
      this.#refresh();
      this.dispatchEvent(new Event("change"));
    });
    textarea.addEventListener("scroll", () => {
      this.gutter.scrollTop = textarea.scrollTop;
    });
    for (const type of ["keyup", "click", "select", "focus"]) {
      textarea.addEventListener(type, () => this.#updateCursor());
    }
    textarea.addEventListener("keydown", (event) => this.#onKeyDown(event));
    textarea.addEventListener("paste", (event) => this.#block(event));
    textarea.addEventListener("drop", (event) => this.#block(event));

    this.#refresh();
  }

  get value() {
    return this.textarea.value;
  }

  set value(code) {
    this.textarea.value = code;
    this.textarea.scrollTop = 0;
    this.#refresh();
  }

  focus() {
    this.textarea.focus();
  }

  #block(event) {
    event.preventDefault();
    this.dispatchEvent(new Event("blocked-paste"));
  }

  #onKeyDown(event) {
    const ta = this.textarea;

    if (event.key === "Escape") {
      this.#escapePressed = true;
      return;
    }

    if (event.key === "Tab") {
      // Esc y luego Tab permite salir del editor usando solo el teclado.
      if (this.#escapePressed) {
        this.#escapePressed = false;
        return;
      }
      event.preventDefault();
      if (event.shiftKey) this.#outdent();
      else this.#insert(INDENT);
      return;
    }
    this.#escapePressed = false;

    if (event.key === "Enter" && !event.ctrlKey && !event.metaKey && !event.shiftKey) {
      event.preventDefault();
      const before = ta.value.slice(0, ta.selectionStart);
      const lineStart = before.lastIndexOf("\n") + 1;
      const indent = /^[ \t]*/.exec(before.slice(lineStart))[0];
      const opensBlock = before.trimEnd().endsWith("{");
      const closesNext = ta.value.slice(ta.selectionEnd).trimStart().startsWith("}") && ta.value.slice(ta.selectionEnd, ta.value.indexOf("}", ta.selectionEnd)).trim() === "";
      if (opensBlock && closesNext) {
        this.#insert(`\n${indent}${INDENT}\n${indent}`);
        const pos = ta.selectionStart - indent.length - 1;
        ta.setSelectionRange(pos, pos);
      } else {
        this.#insert(`\n${indent}${opensBlock ? INDENT : ""}`);
      }
      return;
    }

    // Al escribir "}" en una línea vacía con sangría, se reduce un nivel.
    if (event.key === "}") {
      const before = ta.value.slice(0, ta.selectionStart);
      const lineStart = before.lastIndexOf("\n") + 1;
      const linePrefix = before.slice(lineStart);
      if (linePrefix.length >= INDENT.length && linePrefix.trim() === "") {
        event.preventDefault();
        ta.setSelectionRange(ta.selectionStart - INDENT.length, ta.selectionEnd);
        this.#insert("}");
      }
    }
  }

  #insert(text) {
    const ta = this.textarea;
    ta.focus();
    // execCommand conserva el historial de deshacer (Ctrl+Z) del navegador.
    const ok = document.execCommand && document.execCommand("insertText", false, text);
    if (!ok) {
      ta.setRangeText(text, ta.selectionStart, ta.selectionEnd, "end");
      ta.dispatchEvent(new Event("input"));
    }
  }

  #outdent() {
    const ta = this.textarea;
    const lineStart = ta.value.lastIndexOf("\n", ta.selectionStart - 1) + 1;
    const leading = /^ {1,4}/.exec(ta.value.slice(lineStart));
    if (!leading) return;
    const caret = ta.selectionStart;
    ta.setSelectionRange(lineStart, lineStart + leading[0].length);
    this.#insert("");
    const pos = Math.max(lineStart, caret - leading[0].length);
    ta.setSelectionRange(pos, pos);
  }

  #refresh() {
    const lines = this.textarea.value.split("\n").length;
    if (lines !== this.#lineCount) {
      this.#lineCount = lines;
      const fragment = document.createDocumentFragment();
      for (let i = 1; i <= lines; i++) {
        const span = document.createElement("span");
        span.textContent = String(i);
        fragment.append(span);
      }
      this.gutter.replaceChildren(fragment);
    }
    const chars = this.textarea.value.length;
    this.countLabel.textContent = `${chars} ${chars === 1 ? "carácter" : "caracteres"}`;
    this.fileLabel.textContent = sourceFileName(this.textarea.value);
    this.gutter.scrollTop = this.textarea.scrollTop;
    this.#updateCursor();
  }

  #updateCursor() {
    const before = this.textarea.value.slice(0, this.textarea.selectionStart);
    const line = before.split("\n").length;
    const col = before.length - before.lastIndexOf("\n");
    this.cursorLabel.textContent = `Ln ${line}, Col ${col}`;
    this.gutter.querySelector(".is-current")?.classList.remove("is-current");
    this.gutter.children[line - 1]?.classList.add("is-current");
  }
}
