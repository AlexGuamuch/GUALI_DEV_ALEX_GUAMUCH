/* Formatos y catálogos compartidos. */

export const OBJECTIVES = {
  COMPILAR_CON_ERROR: {
    label: "Compilar con error",
    badge: "badge-danger",
    icon: "warning-circle",
    description: "Presiona Compilar y consigue que el compilador reporte al menos un error.",
  },
  COMPILAR_EXITOSO: {
    label: "Compilar exitoso",
    badge: "badge-success",
    icon: "check-circle",
    description: "Presiona Compilar y consigue 0 errores de compilación.",
  },
  EJECUTAR_CON_ERROR: {
    label: "Ejecutar con error",
    badge: "badge-warning",
    icon: "warning-circle",
    description: "Compila sin errores y ejecuta el programa hasta que produzca un error de ejecución.",
  },
  EJECUTAR_EXITOSO: {
    label: "Ejecutar exitoso",
    badge: "badge-info",
    icon: "play-circle",
    description: "Compila sin errores y ejecuta el programa hasta que termine sin errores.",
  },
};

const pad = (n) => String(n).padStart(2, "0");

/** 15/05/2026 4:30:10 PM */
export function formatDateTime(value) {
  if (!value) return "—";
  const d = new Date(value);
  const hours = d.getHours() % 12 || 12;
  const suffix = d.getHours() < 12 ? "AM" : "PM";
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${hours}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${suffix}`;
}

/** 15/05/2026 */
export function formatDate(value) {
  if (!value) return "—";
  const d = new Date(value);
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function formatNumber(n) {
  return new Intl.NumberFormat("es-GT").format(n);
}

/** Nombre del archivo según el código: clase pública, cualquier clase o app25 (sintaxis compacta). */
export function sourceFileName(code) {
  const match = /public\s+class\s+(\w+)/.exec(code) ?? /\bclass\s+(\w+)/.exec(code);
  return `${match ? match[1] : "app25"}.java`;
}
