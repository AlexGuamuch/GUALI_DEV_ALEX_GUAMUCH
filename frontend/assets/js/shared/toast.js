import { h, icon } from "./dom.js";

const ICONS = {
  success: "check-circle",
  danger: "x-circle",
  warning: "warning-circle",
  info: "info",
};

/** Muestra una notificación breve en la esquina inferior derecha. */
export function toast(message, { type = "info", timeout = 3800 } = {}) {
  const region = document.getElementById("toasts");
  if (!region) return;

  const el = h("div", { class: `toast toast-${type}`, role: type === "danger" ? "alert" : "status" },
    icon(ICONS[type] ?? "info", "fill"),
    h("span", {}, message));
  region.append(el);

  setTimeout(() => {
    el.classList.add("is-leaving");
    el.addEventListener("animationend", () => el.remove(), { once: true });
  }, timeout);
}
