/* Panel de administración: estadísticas, administradores y lecciones. */

import { $, $$, h, icon } from "../shared/dom.js";
import { api } from "../shared/api.js";
import { ImportDialog } from "../shared/import-dialog.js";
import { bindPreferenceControls } from "../shared/preferences.js";
import { toast } from "../shared/toast.js";
import { OBJECTIVES, formatDate, formatDateTime, formatNumber } from "../shared/format.js";
import { ColumnChart } from "./column-chart.js";

const notify = (message, type = "info") => toast(message, { type });

const session = await api.getSession();
if (!session.authenticated) {
  location.replace("login.html");
  throw new Error("Sesión requerida");
}

// Si el token vence o el administrador fue desactivado, se vuelve al login.
document.addEventListener("guali:sesion-expirada", () => location.replace("login.html?expirada=1"));

// Sesión deslizante: mientras la pestaña esté visible se renueva el token antes de que venza.
setInterval(async () => {
  if (document.visibilityState !== "visible") return;
  const s = await api.getSession();
  if (!s.authenticated) location.replace("login.html?expirada=1");
}, 5 * 60 * 1000);

bindPreferenceControls();
initDialogs();
renderMe();

for (const btn of $$('[data-action="logout"]')) {
  btn.addEventListener("click", async () => {
    await api.logout();
    location.replace("login.html");
  });
}

/* ---------- Navegación por módulos ---------- */

const pages = {
  estadisticas: initStats(),
  usuarios: initUsers(),
  lecciones: initLessons(),
};

window.addEventListener("hashchange", route);
route();

function route() {
  const name = location.hash.slice(1) in pages ? location.hash.slice(1) : "estadisticas";
  for (const page of $$("[data-page]")) page.hidden = page.dataset.page !== name;
  for (const link of $$("[data-route]")) {
    if (link.dataset.route === name) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  }
  pages[name].load();
}

function renderMe() {
  const name = session.fullName || session.username;
  $("#me-name").textContent = name;
  $("#me-username").textContent = session.username;
  $("#me-initials").textContent = initials(name);
}

function initDialogs() {
  for (const dialog of $$("dialog.dialog")) {
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog || event.target.closest("[data-close]")) dialog.close();
    });
  }
}

function confirmDialog(title, text, onConfirm) {
  const dialog = $("#dlg-confirm");
  $("#dlg-confirm-title").textContent = title;
  $("#dlg-confirm-text").textContent = text;
  const button = $("#btn-confirm");
  const fresh = button.cloneNode(true);
  button.replaceWith(fresh);
  fresh.addEventListener("click", async () => {
    fresh.disabled = true;
    try {
      await onConfirm();
      dialog.close();
    } finally {
      fresh.disabled = false;
    }
  });
  dialog.showModal();
}

/* ---------- Estadísticas ---------- */

function initStats() {
  const fromInput = $("#range-from");
  const toInput = $("#range-to");
  const chart = new ColumnChart($("#visitors-chart"), {
    value: (d) => d.visitors,
    unit: "visitantes",
    label: (d) => longDate(d.date),
    tick: (d) => d.date.slice(8, 10) + "/" + d.date.slice(5, 7),
  });

  const setPreset = (days) => {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - (days - 1));
    fromInput.value = isoDate(from);
    toInput.value = isoDate(to);
    for (const b of $$("[data-range]")) b.setAttribute("aria-pressed", String(Number(b.dataset.range) === days));
  };

  for (const btn of $$("[data-range]")) {
    btn.addEventListener("click", () => {
      setPreset(Number(btn.dataset.range));
      load();
    });
  }

  $("#range-apply").addEventListener("click", () => {
    if (!fromInput.value || !toInput.value || fromInput.value > toInput.value) {
      notify("Revisa el intervalo: la fecha inicial debe ser anterior a la final.", "warning");
      return;
    }
    for (const b of $$("[data-range]")) b.setAttribute("aria-pressed", "false");
    load();
  });

  for (const btn of $$("[data-chart-view]")) {
    btn.addEventListener("click", () => {
      const table = btn.dataset.chartView === "table";
      $("#visitors-chart").hidden = table;
      $("#visitors-table").hidden = !table;
      for (const b of $$("[data-chart-view]")) b.setAttribute("aria-pressed", String(b === btn));
    });
  }

  setPreset(30);

  async function load() {
    let stats;
    try {
      stats = await api.getStats(fromInput.value, toInput.value);
    } catch (error) {
      notify(error.message, "danger");
      return;
    }
    const { totals, series, topLessons } = stats;
    const days = series.length;

    $("#range-note").textContent = `${days} ${days === 1 ? "día" : "días"} · del ${formatDate(fromInput.value + "T00:00")} al ${formatDate(toInput.value + "T00:00")}`
      + (stats.partial ? " · datos de lecciones no disponibles" : "");
    $("#kpi-visitors").textContent = formatNumber(totals.visitors);
    $("#kpi-visits").textContent = formatNumber(totals.visits);
    $("#kpi-opened").textContent = formatNumber(totals.opened);
    $("#kpi-completed").textContent = formatNumber(totals.completed);
    $("#kpi-rate").textContent = totals.opened
      ? `${Math.round((totals.completed / totals.opened) * 100)} % de los intentos`
      : "Sin intentos";

    chart.update(series);
    $("#visitors-rows").replaceChildren(...[...series].reverse().map((d) => h("tr", {},
      h("td", { class: "cell-mono" }, formatDate(d.date + "T00:00")),
      h("td", { class: "cell-mono" }, formatNumber(d.visitors)),
      h("td", { class: "cell-mono" }, formatNumber(d.visits)),
      h("td", { class: "cell-mono" }, formatNumber(d.opened)),
      h("td", { class: "cell-mono" }, formatNumber(d.completed)))));

    const top = Math.max(1, ...topLessons.map((l) => l.opened));
    if (!topLessons.length) {
      $("#top-lessons").replaceChildren(h("li", { class: "muted" }, "Sin intentos en este intervalo."));
      return;
    }
    $("#top-lessons").replaceChildren(...topLessons.map((l) => {
      const fill = h("span", { class: "rank-fill" });
      fill.style.width = `${Math.max(4, (l.opened / top) * 82)}%`;
      return h("li", {},
        h("span", { class: "rank-name", title: l.name }, l.name),
        h("span", { class: "rank-bar" }, fill, formatNumber(l.opened)));
    }));
  }

  return { load };
}

/* ---------- Administradores ---------- */

function initUsers() {
  const rows = $("#user-rows");
  const search = $("#user-search");
  const dialog = $("#dlg-user");
  const form = $("#user-form");
  let users = [];
  let editing = null;

  search.addEventListener("input", render);
  $("#btn-new-user").addEventListener("click", () => openForm(null));
  form.addEventListener("submit", save);

  async function load() {
    rows.replaceChildren(h("tr", {}, h("td", { colspan: "5", class: "table-empty" }, "Cargando…")));
    try {
      users = await api.listUsers();
    } catch (error) {
      notify(error.message, "danger");
      users = [];
    }
    render();
  }

  function render() {
    const q = search.value.trim().toLowerCase();
    const list = users.filter((u) => !q || u.username.includes(q) || u.fullName.toLowerCase().includes(q));
    $("#user-count").textContent = `${list.length} de ${users.length} administradores`;

    if (!list.length) {
      rows.replaceChildren(h("tr", {}, h("td", { colspan: "5", class: "table-empty" }, "No hay administradores que coincidan.")));
      return;
    }

    rows.replaceChildren(...list.map((u) => {
      const isMe = u.username === session.username;
      return h("tr", {},
        h("td", {},
          h("div", { class: "user-cell" },
            h("span", { class: "avatar" }, initials(u.fullName)),
            h("span", { class: "user-cell-text" },
              h("strong", {}, u.fullName, isMe ? h("span", { class: "you-tag" }, "(tú)") : null),
              h("span", {}, u.username)))),
        h("td", {}, u.active
          ? h("span", { class: "badge badge-success" }, "Activo")
          : h("span", { class: "badge" }, "Inactivo")),
        h("td", { class: "cell-mono hide-sm" }, u.lastLogin ? formatDateTime(u.lastLogin) : "Nunca"),
        h("td", { class: "cell-mono hide-sm" }, formatDate(u.createdAt)),
        h("td", { class: "cell-actions" },
          h("button", { class: "icon-btn", type: "button", title: "Editar", "aria-label": `Editar a ${u.fullName}`, onclick: () => openForm(u) }, icon("pencil-simple")),
          h("button", {
            class: "icon-btn is-danger",
            type: "button",
            title: isMe ? "No puedes eliminar tu propia cuenta" : "Eliminar",
            "aria-label": `Eliminar a ${u.fullName}`,
            disabled: isMe,
            onclick: () => confirmDialog(
              "¿Eliminar administrador?",
              `${u.fullName} (${u.username}) ya no podrá iniciar sesión. Esta acción no se puede deshacer.`,
              async () => {
                try {
                  await api.deleteUser(u.id);
                  notify(`Administrador eliminado: ${u.username}`, "success");
                  load();
                } catch (error) {
                  notify(error.message, "danger");
                }
              }),
          }, icon("trash"))));
    }));
  }

  function openForm(user) {
    editing = user;
    form.reset();
    clearErrors();
    $("#dlg-user-title").textContent = user ? "Editar administrador" : "Nuevo administrador";
    $("#dlg-user-sub").textContent = user
      ? "Deja la contraseña vacía para conservar la actual."
      : "Podrá iniciar sesión y gestionar lecciones y usuarios.";
    form.fullName.value = user?.fullName ?? "";
    form.username.value = user?.username ?? "";
    form.active.checked = user?.active ?? true;
    form.active.disabled = user?.username === session.username;
    dialog.showModal();
    form.fullName.focus();
  }

  async function save(event) {
    event.preventDefault();
    clearErrors();
    const data = {
      fullName: form.fullName.value.trim(),
      username: form.username.value.trim().toLowerCase(),
      password: form.password.value,
      confirm: form.confirm.value,
      active: form.active.checked,
    };

    const errors = {};
    if (data.fullName.length < 3) errors.fullName = "Escribe el nombre completo.";
    if (!/^[a-z0-9._-]{3,30}$/.test(data.username)) errors.username = "Usa de 3 a 30 caracteres: minúsculas, números, punto, guion o guion bajo.";
    else if (users.some((u) => u.username === data.username && u.id !== editing?.id)) errors.username = "Ese usuario ya existe.";
    if (!editing || data.password) {
      if (data.password.length < 12 || !/[A-Z]/.test(data.password) || !/[a-z]/.test(data.password) || !/\d/.test(data.password)) {
        errors.password = "Mínimo 12 caracteres, con mayúscula, minúscula y número.";
      } else if (data.password !== data.confirm) {
        errors.confirm = "Las contraseñas no coinciden.";
      }
    }

    if (Object.keys(errors).length) {
      for (const [field, message] of Object.entries(errors)) {
        const el = $(`[data-error="${field}"]`, form);
        el.textContent = message;
        el.hidden = false;
        form[field].setAttribute("aria-invalid", "true");
      }
      form[Object.keys(errors)[0]].focus();
      return;
    }

    const button = $("#btn-save-user");
    button.disabled = true;
    try {
      await api.saveUser({
        id: editing?.id,
        fullName: data.fullName,
        username: data.username,
        password: data.password,
        active: data.active,
      });
      dialog.close();
      notify(editing ? `Cambios guardados: ${data.username}` : `Administrador creado: ${data.username}`, "success");
      load();
    } catch (error) {
      notify(error.message, "danger");
    } finally {
      button.disabled = false;
    }
  }

  function clearErrors() {
    for (const el of $$("[data-error]", form)) el.hidden = true;
    for (const el of $$("[aria-invalid]", form)) el.removeAttribute("aria-invalid");
  }

  return { load };
}

/* ---------- Lecciones ---------- */

function initLessons() {
  const rows = $("#lesson-rows");
  const importDialog = new ImportDialog(api, { notify, onImported: () => load() });

  $("#btn-import-open").addEventListener("click", () => importDialog.show());

  $("#btn-export").addEventListener("click", async () => {
    try {
      const { fileName } = await api.exportLessons();
      notify(`Descarga lista: ${fileName}`, "success");
    } catch (error) {
      notify(error.message, "danger");
    }
  });

  async function load() {
    rows.replaceChildren(h("tr", {}, h("td", { colspan: "6", class: "table-empty" }, "Cargando…")));
    let lessons;
    try {
      lessons = await api.listLessonsAdmin();
    } catch (error) {
      notify(error.message, "danger");
      lessons = [];
    }
    if (!lessons.length) {
      rows.replaceChildren(h("tr", {}, h("td", { colspan: "6", class: "table-empty" }, "No hay lecciones. Importa un compendio para empezar.")));
      return;
    }
    rows.replaceChildren(...lessons.map((l, i) => {
      const objective = OBJECTIVES[l.objective];
      return h("tr", {},
        h("td", { class: "cell-mono" }, String(i + 1).padStart(2, "0")),
        h("td", { class: "cell-mono" }, l.name),
        h("td", {}, h("span", { class: `badge no-dot ${objective.badge}` }, objective.label)),
        h("td", { class: "cell-mono hide-sm" }, String(l.images)),
        h("td", { class: "cell-mono hide-sm" }, formatDateTime(l.updatedAt)),
        h("td", { class: "cell-actions" },
          h("button", {
            class: "icon-btn is-danger",
            type: "button",
            title: "Eliminar",
            "aria-label": `Eliminar ${l.name}`,
            onclick: () => confirmDialog(
              "¿Eliminar lección?",
              `${l.name} y sus imágenes se borran de la base de datos. El historial de intentos se conserva.`,
              async () => {
                try {
                  await api.deleteLesson(l.id);
                  notify(`Lección eliminada: ${l.name}`, "success");
                  load();
                } catch (error) {
                  notify(error.message, "danger");
                }
              }),
          }, icon("trash"))));
    }));
  }

  return { load };
}

/* ---------- Utilidades ---------- */

function initials(name) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join("") || "?";
}

function isoDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function longDate(iso) {
  return new Date(iso + "T00:00").toLocaleDateString("es-GT", { weekday: "long", day: "numeric", month: "long" });
}
