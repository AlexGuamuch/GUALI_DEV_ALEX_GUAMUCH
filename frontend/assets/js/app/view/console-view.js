/* Vista: panel de consola con la salida de compilación y ejecución. */

import { h, icon } from "../../shared/dom.js";

export class ConsoleView {
  constructor(el) {
    this.el = el;
    this.placeholder = el.firstElementChild.cloneNode(true);
  }

  clear() {
    this.el.replaceChildren(this.placeholder.cloneNode(true));
  }

  /** kind: "meta" | "error" | "success" | "warning" | undefined */
  print(text, kind) {
    this.el.querySelector(".console-placeholder")?.remove();
    const limpio = String(text ?? "").replace(/\s+$/, "");
    if (!limpio) return;
    this.el.append(h("span", { class: `console-line${kind ? ` is-${kind}` : ""}` }, limpio));
    this.#scroll();
  }

  goal(message) {
    this.el.append(h("span", { class: "console-goal" }, icon("check-circle", "fill"), message));
    this.#scroll();
  }

  #scroll() {
    this.el.scrollTop = this.el.scrollHeight;
  }
}
