/* Pantalla de acceso de administradores. */

import { $ } from "../shared/dom.js";
import { api } from "../shared/api.js";

const form = $("#login-form");
const errorBox = $("#login-error");
const submit = $("#login-submit");
const password = $("#password");
const toggle = $("#toggle-password");

const session = await api.getSession();
if (session.authenticated) location.replace("admin.html");
if (new URLSearchParams(location.search).has("expirada")) {
  showError("Tu sesión expiró por inactividad. Vuelve a iniciar sesión.");
}

toggle.addEventListener("click", () => {
  const visible = password.type === "password";
  password.type = visible ? "text" : "password";
  toggle.setAttribute("aria-pressed", String(visible));
  toggle.setAttribute("aria-label", visible ? "Ocultar contraseña" : "Mostrar contraseña");
  toggle.firstElementChild.className = `ph-bold ${visible ? "ph-eye-slash" : "ph-eye"}`;
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  errorBox.hidden = true;

  const username = $("#username").value.trim();
  if (!username || !password.value) {
    showError("Ingresa tu usuario y tu contraseña.");
    return;
  }

  submit.disabled = true;
  const iconEl = submit.querySelector("i");
  iconEl.className = "ph-bold ph-circle-notch spin";
  try {
    await api.login(username, password.value);
    location.replace("admin.html");
  } catch (error) {
    // Mensaje genérico: no revela si el usuario existe.
    showError(error.message || "Usuario o contraseña incorrectos.");
    password.value = "";
    password.focus();
  } finally {
    submit.disabled = false;
    iconEl.className = "ph-bold ph-sign-in";
  }
});

function showError(message) {
  errorBox.querySelector("span").textContent = message;
  errorBox.hidden = false;
}
