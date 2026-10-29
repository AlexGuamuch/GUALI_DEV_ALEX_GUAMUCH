import { $, $$ } from "./dom.js";

/**
 * Menús desplegables accesibles para la barra de menú.
 * Cada [data-menu] contiene un .menu-trigger y un .menu-popover.
 */
export function initMenus(root = document) {
  const menus = $$("[data-menu]", root);

  const closeAll = (except) => {
    for (const menu of menus) {
      if (menu !== except) setOpen(menu, false);
    }
  };

  for (const menu of menus) {
    const trigger = $(".menu-trigger", menu);
    const popover = $(".menu-popover", menu);

    trigger.addEventListener("click", () => {
      const open = trigger.getAttribute("aria-expanded") !== "true";
      closeAll(menu);
      setOpen(menu, open);
      if (open) focusFirst(popover);
    });

    // Al pasar el mouse con otro menú abierto, se cambia de menú (como en un escritorio).
    trigger.addEventListener("mouseenter", () => {
      const anyOpen = menus.some((m) => m !== menu && $(".menu-trigger", m).getAttribute("aria-expanded") === "true");
      if (anyOpen) {
        closeAll(menu);
        setOpen(menu, true);
      }
    });

    popover.addEventListener("keydown", (event) => {
      const items = $$(".menu-item:not(:disabled)", popover).filter((i) => i.offsetParent !== null);
      const index = items.indexOf(document.activeElement);
      if (event.key === "ArrowDown") {
        event.preventDefault();
        items[(index + 1) % items.length]?.focus();
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        items[(index - 1 + items.length) % items.length]?.focus();
      }
    });

    // Las opciones con data-action cierran el menú; los controles de una fila (stepper, tema) no.
    popover.addEventListener("click", (event) => {
      if (event.target.closest(".menu-item")) setOpen(menu, false);
    });
  }

  document.addEventListener("click", (event) => {
    if (!event.target.closest("[data-menu]")) closeAll();
  });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    const open = menus.find((m) => $(".menu-trigger", m).getAttribute("aria-expanded") === "true");
    if (open) {
      setOpen(open, false);
      $(".menu-trigger", open).focus();
    }
  });
}

function setOpen(menu, open) {
  $(".menu-trigger", menu).setAttribute("aria-expanded", String(open));
  $(".menu-popover", menu).hidden = !open;
}

function focusFirst(popover) {
  const first = $$(".menu-item:not(:disabled)", popover).find((i) => i.offsetParent !== null);
  first?.focus({ preventScroll: true });
}
