/* Cliente de la API REST (gateway en /api). Traduce el JSON del servidor al formato que usan las vistas.
   - Cookies: la sesión de administrador viaja en una cookie HttpOnly que JavaScript no puede leer.
   - CSRF: en POST/PUT/DELETE se envía la cabecera X-XSRF-TOKEN con el valor de la cookie XSRF-TOKEN. */

const XSRF_COOKIE = "XSRF-TOKEN";

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const MENSAJES = {
  401: "Tu sesión expiró. Vuelve a iniciar sesión.",
  403: "No tienes permiso para esta acción. Recarga la página e inténtalo de nuevo.",
  404: "No se encontró lo que buscabas.",
  413: "El archivo es demasiado grande.",
  429: "Demasiadas solicitudes. Espera un minuto e inténtalo de nuevo.",
  503: "El servicio está ocupado. Intenta de nuevo en unos segundos.",
};

function leerCookie(nombre) {
  const par = document.cookie.split("; ").find((c) => c.startsWith(`${nombre}=`));
  return par ? decodeURIComponent(par.slice(nombre.length + 1)) : null;
}

export class HttpApi {
  demo = false;

  /* ---------- Infraestructura ---------- */

  async #tokenCsrf(renovar = false) {
    let token = renovar ? null : leerCookie(XSRF_COOKIE);
    if (!token) {
      await fetch("/api/auth/sesion", { credentials: "same-origin", headers: { Accept: "application/json" } });
      token = leerCookie(XSRF_COOKIE);
    }
    return token ?? "";
  }

  async #pedir(metodo, url, { json, form, keepalive = false, crudo = false } = {}, reintento = false) {
    const headers = { Accept: "application/json" };
    if (metodo !== "GET") headers["X-XSRF-TOKEN"] = await this.#tokenCsrf(reintento);
    let body;
    if (json !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(json);
    } else if (form) {
      body = form;
    }

    let res;
    try {
      res = await fetch(url, { method: metodo, headers, body, credentials: "same-origin", keepalive });
    } catch {
      throw new ApiError(0, "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.");
    }

    // Token CSRF vencido o ausente: se renueva una vez y se repite la solicitud.
    if (res.status === 403 && metodo !== "GET" && !reintento) {
      return this.#pedir(metodo, url, { json, form, keepalive, crudo }, true);
    }
    if (res.status === 401 && url.startsWith("/api/admin/")) {
      document.dispatchEvent(new CustomEvent("guali:sesion-expirada"));
    }
    if (!res.ok) {
      const problema = await res.json().catch(() => null);
      throw new ApiError(res.status, problema?.detail ?? MENSAJES[res.status] ?? "Ocurrió un error inesperado. Inténtalo de nuevo.");
    }
    if (crudo) return res;
    if (res.status === 204) return null;
    return (res.headers.get("Content-Type") ?? "").includes("json") ? res.json() : null;
  }

  /** false si la página no está servida por Guali (por ejemplo, abierta desde IntelliJ o como archivo). */
  async disponible() {
    try {
      const res = await fetch("/api/auth/sesion", { credentials: "same-origin", headers: { Accept: "application/json" } });
      return res.ok && (res.headers.get("Content-Type") ?? "").includes("json");
    } catch {
      return false;
    }
  }

  /* ---------- Sesión de administrador ---------- */

  async getSession() {
    try {
      const s = await this.#pedir("GET", "/api/auth/sesion");
      return s?.authenticated
        ? { authenticated: true, username: s.username, fullName: s.fullName, expiresAt: s.expiraEn }
        : { authenticated: false };
    } catch {
      return { authenticated: false };
    }
  }

  async login(username, password) {
    const s = await this.#pedir("POST", "/api/auth/login", { json: { username, password } });
    return { authenticated: true, username: s.username, fullName: s.fullName };
  }

  async logout() {
    await this.#pedir("POST", "/api/auth/logout");
  }

  /* ---------- Lecciones ---------- */

  async listLessons() {
    const lista = await this.#pedir("GET", "/api/lecciones");
    return lista.map((l) => ({ id: l.id, name: l.nombre, objective: l.objetivo, summary: l.resumen }));
  }

  async getLesson(id) {
    const l = await this.#pedir("GET", `/api/lecciones/${encodeURIComponent(id)}`);
    return { id: l.id, name: l.nombre, objective: l.objetivo, summary: l.resumen, html: l.html };
  }

  async listLessonsAdmin() {
    const lista = await this.#pedir("GET", "/api/admin/lecciones");
    return lista.map((l) => ({
      id: l.id,
      name: l.nombre,
      objective: l.objetivo,
      images: l.imagenes,
      createdAt: l.creadaEn,
      updatedAt: l.actualizadaEn,
    }));
  }

  async importBundle(file) {
    const form = new FormData();
    form.append("archivo", file);
    const r = await this.#pedir("POST", "/api/admin/lecciones/importar", { form });
    return {
      created: r.creadas,
      updated: r.actualizadas,
      rejected: r.rechazadas.map((x) => ({ lesson: x.leccion, reason: x.motivo })),
      warnings: r.advertencias,
    };
  }

  /** Descarga el .zip con todas las lecciones. */
  async exportLessons() {
    const res = await this.#pedir("GET", "/api/admin/lecciones/exportar", { crudo: true });
    const blob = await res.blob();
    const disposicion = res.headers.get("Content-Disposition") ?? "";
    const nombre = /filename="?([^";]+)"?/.exec(disposicion)?.[1] ?? "lecciones.zip";
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = nombre;
    document.body.append(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
    return { fileName: nombre, size: blob.size };
  }

  async deleteLesson(id) {
    await this.#pedir("DELETE", `/api/admin/lecciones/${encodeURIComponent(id)}`);
  }

  /* ---------- Historial ---------- */

  async startAttempt(lesson) {
    const i = await this.#pedir("POST", "/api/intentos", { json: { leccionId: lesson.id } });
    return { id: i.id, startedAt: i.fechaInicio };
  }

  /** keepalive permite que la solicitud termine aunque se cierre la pestaña. */
  async finishAttempt(id, { keepalive = false } = {}) {
    await this.#pedir("POST", `/api/intentos/${encodeURIComponent(id)}/finalizar`, { keepalive }).catch(() => {});
  }

  async listHistory() {
    const lista = await this.#pedir("GET", "/api/intentos");
    return lista.map((i) => ({
      id: i.id,
      lessonName: i.leccionNombre,
      startedAt: i.fechaInicio,
      endedAt: i.fechaFin,
      status: i.estado,
    }));
  }

  /* ---------- Compilar y ejecutar ---------- */

  async compile(code, attemptId) {
    const r = await this.#pedir("POST", "/api/codigo/compilar", { json: { codigo: code, intentoId: attemptId ?? null } });
    return {
      success: r.compilacion.exitosa,
      output: r.compilacion.salida,
      fileName: r.compilacion.archivo,
      durationMs: r.compilacion.duracionMs,
      objectiveMet: r.objetivoCumplido,
    };
  }

  async execute(code, attemptId) {
    const r = await this.#pedir("POST", "/api/codigo/ejecutar", { json: { codigo: code, intentoId: attemptId ?? null } });
    const e = r.ejecucion;
    return {
      compileFailed: !r.compilacion.exitosa,
      compileOutput: r.compilacion.salida,
      success: Boolean(e?.exitosa),
      stdout: e?.salida ?? "",
      stderr: e?.errores ?? "",
      timeout: Boolean(e?.tiempoAgotado),
      durationMs: e?.duracionMs ?? r.compilacion.duracionMs,
      objectiveMet: r.objetivoCumplido,
    };
  }

  /* ---------- Preferencias y visitas ---------- */

  async getPreferences() {
    const p = await this.#pedir("GET", "/api/preferencias").catch(() => null);
    return p ? { theme: p.tema, fontSize: p.tamanoFuente } : null;
  }

  async savePreferences({ theme, fontSize }) {
    await this.#pedir("PUT", "/api/preferencias", { json: { tema: theme, tamanoFuente: fontSize } });
  }

  async registerVisit() {
    await this.#pedir("POST", "/api/visitas").catch(() => {});
  }

  /* ---------- Administradores ---------- */

  async listUsers() {
    const lista = await this.#pedir("GET", "/api/admin/usuarios");
    return lista.map((u) => ({
      id: u.id,
      username: u.username,
      fullName: u.nombreCompleto,
      active: u.activo,
      lastLogin: u.ultimoAcceso,
      createdAt: u.creadoEn,
    }));
  }

  async saveUser({ id, fullName, username, password, active }) {
    const json = { nombreCompleto: fullName, username, password: password || null, activo: active };
    if (id) await this.#pedir("PUT", `/api/admin/usuarios/${encodeURIComponent(id)}`, { json });
    else await this.#pedir("POST", "/api/admin/usuarios", { json });
  }

  async deleteUser(id) {
    await this.#pedir("DELETE", `/api/admin/usuarios/${encodeURIComponent(id)}`);
  }

  /* ---------- Estadísticas ---------- */

  async getStats(from, to) {
    const q = new URLSearchParams({ desde: from, hasta: to });
    const r = await this.#pedir("GET", `/api/admin/estadisticas?${q}`);
    return {
      series: r.porDia.map((d) => ({
        date: d.fecha,
        visitors: d.visitantes,
        visits: d.visitas,
        opened: d.abiertas,
        completed: d.completadas,
      })),
      totals: {
        visitors: r.totales.visitantes,
        visits: r.totales.visitas,
        opened: r.totales.abiertas,
        completed: r.totales.completadas,
      },
      topLessons: r.masAbiertas.map((l) => ({ name: l.nombre, opened: l.abiertas })),
      partial: r.parcial,
    };
  }
}
