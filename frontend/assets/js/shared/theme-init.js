/* Aplica tema y tamaño de fuente antes del primer pintado para evitar parpadeos.
   Script clásico (no módulo) cargado en <head>. */
(function () {
  var root = document.documentElement;
  try {
    var theme = localStorage.getItem("guali.theme");
    if (theme !== "light" && theme !== "dark") {
      theme = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }
    root.setAttribute("data-theme", theme);

    var size = parseInt(localStorage.getItem("guali.fontSize"), 10);
    if (size >= 12 && size <= 24) {
      root.style.setProperty("--fs-work", size + "px");
    }
  } catch (e) {
    root.setAttribute("data-theme", "light");
  }
})();
