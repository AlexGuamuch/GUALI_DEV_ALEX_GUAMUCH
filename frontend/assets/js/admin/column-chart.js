/* Gráfica de columnas de una sola serie, en SVG.
   Columnas de máximo 24px con extremo redondeado de 4px, separación de 2px,
   cuadrícula tenue, etiqueta solo en el máximo y tooltip por columna (mouse y teclado). */

import { h } from "../shared/dom.js";

const SVG = "http://www.w3.org/2000/svg";
const HEIGHT = 260;
const M = { top: 22, right: 8, bottom: 28, left: 40 };

const svg = (tag, attrs = {}) => {
  const el = document.createElementNS(SVG, tag);
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, String(v));
  return el;
};

export class ColumnChart {
  #data = [];
  #active = -1;
  #bars = [];

  /**
   * @param {HTMLElement} container
   * @param {{ value: (d) => number, label: (d) => string, tick: (d) => string, unit: string }} opts
   */
  constructor(container, opts) {
    this.container = container;
    this.opts = opts;
    this.tip = h("div", { class: "chart-tip", hidden: true }, h("strong"), h("span"));

    container.addEventListener("keydown", (event) => {
      if (!this.#data.length) return;
      if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
        event.preventDefault();
        const step = event.key === "ArrowRight" ? 1 : -1;
        const start = this.#active < 0 ? (step > 0 ? -1 : this.#data.length) : this.#active;
        this.#activate(Math.min(this.#data.length - 1, Math.max(0, start + step)));
      } else if (event.key === "Escape") {
        this.#activate(-1);
      }
    });
    container.addEventListener("blur", () => this.#activate(-1));
    new ResizeObserver(() => this.#render()).observe(container);
  }

  update(data) {
    this.#data = data;
    this.#active = -1;
    this.#render();
  }

  #render() {
    const width = this.container.clientWidth;
    if (!width) return;
    const data = this.#data;
    const { value, tick } = this.opts;

    const innerW = width - M.left - M.right;
    const innerH = HEIGHT - M.top - M.bottom;
    const max = niceMax(Math.max(1, ...data.map(value)));
    const y = (v) => M.top + innerH - (v / max) * innerH;
    const slot = innerW / Math.max(1, data.length);
    const barW = Math.max(2, Math.min(24, slot - 2));

    const root = svg("svg", { viewBox: `0 0 ${width} ${HEIGHT}`, width, height: HEIGHT, "aria-hidden": "true" });

    // Cuadrícula y eje Y (valores redondos)
    for (let i = 0; i <= 4; i++) {
      const v = (max / 4) * i;
      const yy = Math.round(y(v)) + 0.5;
      root.append(svg("line", { class: i === 0 ? "baseline" : "grid-line", x1: M.left, x2: width - M.right, y1: yy, y2: yy }));
      const label = svg("text", { class: "axis-label", x: M.left - 8, y: yy + 3.5, "text-anchor": "end" });
      label.textContent = compact(v);
      root.append(label);
    }

    // Etiquetas del eje X: unas 6 fechas repartidas
    const every = Math.max(1, Math.ceil(data.length / 6));
    data.forEach((d, i) => {
      if (i % every !== 0 && i !== data.length - 1) return;
      if (i === data.length - 1 && i % every !== 0 && i % every < every / 2) return;
      const t = svg("text", { class: "axis-label", x: M.left + slot * i + slot / 2, y: HEIGHT - 8, "text-anchor": "middle" });
      t.textContent = tick(d);
      root.append(t);
    });

    // Columnas: crecen desde la línea base, extremo superior redondeado
    const peak = data.reduce((best, d, i) => (value(d) > value(data[best] ?? d) ? i : best), 0);
    this.#bars = data.map((d, i) => {
      const x = M.left + slot * i + (slot - barW) / 2;
      const top = y(value(d));
      const bar = svg("path", { class: "bar", d: columnPath(x, top, barW, y(0)) });
      root.append(bar);
      return bar;
    });

    // Etiqueta directa solo en el máximo
    if (data.length) {
      const px = M.left + slot * peak + slot / 2;
      const label = svg("text", { class: "value-label", x: px, y: y(value(data[peak])) - 7, "text-anchor": "middle" });
      label.textContent = compact(value(data[peak]));
      root.append(label);
    }

    // Zonas de interacción: más grandes que la columna (todo el carril)
    data.forEach((d, i) => {
      const hit = svg("rect", { class: "hit", x: M.left + slot * i, y: M.top, width: slot, height: innerH });
      hit.addEventListener("pointerenter", () => this.#activate(i));
      hit.addEventListener("pointerleave", () => this.#activate(-1));
      root.append(hit);
    });

    this.geometry = { slot, y };
    this.container.replaceChildren(root, this.tip);
    if (this.#active >= 0) this.#activate(this.#active);
  }

  #activate(index) {
    this.#bars[this.#active]?.classList.remove("is-active");
    this.#active = index;
    if (index < 0 || !this.#data[index]) {
      this.tip.hidden = true;
      return;
    }
    const d = this.#data[index];
    this.#bars[index].classList.add("is-active");
    this.tip.firstChild.textContent = `${this.opts.value(d).toLocaleString("es-GT")} ${this.opts.unit}`;
    this.tip.lastChild.textContent = this.opts.label(d);
    const { slot, y } = this.geometry;
    const left = M.left + slot * index + slot / 2;
    this.tip.style.left = `${Math.min(this.container.clientWidth - 70, Math.max(70, left))}px`;
    this.tip.style.top = `${y(this.opts.value(d))}px`;
    this.tip.hidden = false;
  }
}

function columnPath(x, top, w, base) {
  const height = base - top;
  if (height <= 0) return "";
  const r = Math.min(4, w / 2, height);
  return [
    `M${x},${base}`,
    `V${top + r}`,
    `Q${x},${top} ${x + r},${top}`,
    `H${x + w - r}`,
    `Q${x + w},${top} ${x + w},${top + r}`,
    `V${base}`,
    "Z",
  ].join(" ");
}

/** Máximo del eje tal que las 4 divisiones caigan en números redondos. */
function niceMax(v) {
  const raw = v / 4;
  const exp = 10 ** Math.floor(Math.log10(raw));
  const n = raw / exp;
  const step = (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * exp;
  return Math.max(4, step * 4);
}

function compact(v) {
  return v >= 1000 ? `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}K` : String(Math.round(v));
}
