/* Preferencias del usuario: tema y tamaño de fuente del área de trabajo. */

import { $, $$ } from "./dom.js";
import { api } from "./api.js";

export const FONT_MIN = 12;
export const FONT_MAX = 24;
const FONT_DEFAULT = 15;

const root = document.documentElement;
let persistTimer;

export function getTheme() {
  return root.dataset.theme === "dark" ? "dark" : "light";
}

export function getFontSize() {
  const value = parseInt(getComputedStyle(root).getPropertyValue("--fs-work"), 10);
  return Number.isFinite(value) ? value : FONT_DEFAULT;
}

export function setTheme(theme) {
  root.dataset.theme = theme;
  store("guali.theme", theme);
  syncControls();
  persist();
}

export function setFontSize(px) {
  const size = Math.min(FONT_MAX, Math.max(FONT_MIN, px));
  root.style.setProperty("--fs-work", `${size}px`);
  store("guali.fontSize", String(size));
  syncControls();
  persist();
  document.dispatchEvent(new CustomEvent("guali:font-size", { detail: size }));
}

/** Conecta los botones [data-theme-choice] y [data-action=font-up|font-down]. */
export function bindPreferenceControls(scope = document) {
  for (const btn of $$("[data-theme-choice]", scope)) {
    btn.addEventListener("click", () => setTheme(btn.dataset.themeChoice));
  }
  $('[data-action="font-up"]', scope)?.addEventListener("click", () => setFontSize(getFontSize() + 1));
  $('[data-action="font-down"]', scope)?.addEventListener("click", () => setFontSize(getFontSize() - 1));
  syncControls();
}

/** Aplica las preferencias guardadas en el servidor para este navegador (si existen). */
export async function loadServerPreferences() {
  const prefs = await api.getPreferences().catch(() => null);
  if (!prefs) return;
  root.dataset.theme = prefs.theme;
  store("guali.theme", prefs.theme);
  root.style.setProperty("--fs-work", `${prefs.fontSize}px`);
  store("guali.fontSize", String(prefs.fontSize));
  syncControls();
  document.dispatchEvent(new CustomEvent("guali:font-size", { detail: prefs.fontSize }));
}

function syncControls() {
  const theme = getTheme();
  for (const btn of $$("[data-theme-choice]")) {
    btn.setAttribute("aria-pressed", String(btn.dataset.themeChoice === theme));
  }
  const size = getFontSize();
  const output = $("#font-size-value");
  if (output) output.textContent = `${size} px`;
  const up = $('[data-action="font-up"]');
  const down = $('[data-action="font-down"]');
  if (up) up.disabled = size >= FONT_MAX;
  if (down) down.disabled = size <= FONT_MIN;
}

function store(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* sin almacenamiento local: se mantiene en memoria */
  }
}

function persist() {
  clearTimeout(persistTimer);
  persistTimer = setTimeout(() => {
    api.savePreferences({ theme: getTheme(), fontSize: getFontSize() }).catch(() => {});
  }, 500);
}
