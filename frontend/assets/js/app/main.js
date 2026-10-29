/* Punto de entrada de la interfaz principal (patrón MVC en el cliente). */

import { $ } from "../shared/dom.js";
import { api } from "../shared/api.js";
import { ImportDialog } from "../shared/import-dialog.js";
import { initMenus } from "../shared/menus.js";
import { bindPreferenceControls, loadServerPreferences } from "../shared/preferences.js";
import { toast } from "../shared/toast.js";
import { WorkspaceModel } from "./model/workspace-model.js";
import { EditorView } from "./view/editor-view.js";
import { ConsoleView } from "./view/console-view.js";
import { LessonView } from "./view/lesson-view.js";
import { DeleteLessonDialog, HelpDialog, HistoryDialog, OpenLessonDialog, initDialogs } from "./view/dialogs.js";
import { WorkspaceController } from "./controller/workspace-controller.js";

const notify = (message, type = "info") => toast(message, { type });

const model = new WorkspaceModel();

const editor = new EditorView({
  textarea: $("#editor"),
  gutter: $("#editor-gutter"),
  cursorLabel: $("#cursor-pos"),
  countLabel: $("#char-count"),
  fileLabel: $("#editor-file"),
});

const lessonView = new LessonView({
  title: $("#lesson-title"),
  meta: $("#lesson-meta"),
  body: $("#lesson-body"),
});

const consoleView = new ConsoleView($("#console"));

let controller;
const dialogs = {
  openLesson: new OpenLessonDialog(api, (id) => controller.openLesson(id)),
  history: new HistoryDialog(api),
  help: new HelpDialog(),
  importBundle: new ImportDialog(api, { notify }),
  deleteLesson: new DeleteLessonDialog(api, {
    notify,
    onDeleted: (id, name) => {
      notify(`Lección eliminada: ${name}`, "success");
      if (model.lesson?.id === id) controller.closeLesson();
    },
  }),
};

controller = new WorkspaceController({ api, model, lessonView, editor, consoleView, dialogs });

initMenus();
initDialogs();
bindPreferenceControls();
controller.init();
loadServerPreferences();
api.registerVisit();
