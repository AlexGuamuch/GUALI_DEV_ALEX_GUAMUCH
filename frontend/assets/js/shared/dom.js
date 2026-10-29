/* Utilidades DOM. Todo el contenido dinámico se crea con nodos y textContent,
   nunca concatenando HTML con datos del usuario. */

export const $ = (selector, root = document) => root.querySelector(selector);
export const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

/**
 * Crea un elemento.
 * h("button", { class: "btn", onclick: fn, dataset: { id: 1 } }, "Texto", otroNodo)
 */
export function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (value === undefined || value === null || value === false) continue;
    if (key === "class") el.className = value;
    else if (key === "dataset") Object.assign(el.dataset, value);
    else if (key.startsWith("on") && typeof value === "function") el.addEventListener(key.slice(2), value);
    else if (value === true) el.setAttribute(key, "");
    else el.setAttribute(key, String(value));
  }
  append(el, children);
  return el;
}

function append(el, children) {
  for (const child of children.flat()) {
    if (child === undefined || child === null || child === false) continue;
    el.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
}

export function icon(name, weight = "bold") {
  return h("i", { class: `ph-${weight} ph-${name}`, "aria-hidden": "true" });
}

export function show(el, visible) {
  el.hidden = !visible;
}
