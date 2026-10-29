/* Punto único de acceso a datos para las vistas (cliente REST del gateway). */

import { HttpApi } from "./http-api.js";

export { ApiError } from "./http-api.js";

export const api = new HttpApi();
