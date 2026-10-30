# Decisiones de requerimientos (justificadas)

## Funcionales

| # | Decisión | Justificación |
|---|---|---|
| F1 | Los estudiantes no tienen cuenta; cada navegador recibe un **identificador anónimo** en una cookie HttpOnly (`guali_cliente`) asignada por el gateway. | El enunciado pide uso sin autenticación, pero el historial y la configuración deben persistir. El identificador permite guardarlos en la base de datos sin datos personales. |
| F2 | El **historial** registra un intento cada vez que se abre una lección. La fecha final se registra al completar el objetivo, al cerrar la lección, al abrir otra o al cerrar la pestaña. Los intentos abiertos más de 6 h se cierran automáticamente. | Cumple "fecha en que la lección se completó, abandonó o canceló" y "registra todos los intentos, no solo el exitoso". |
| F3 | El **objetivo se evalúa en el servidor** (lecciones-service) con el resultado real del compilador, no en el navegador. | El navegador puede manipularse; el servidor es la fuente confiable del estado "Completada". |
| F4 | Un programa detenido por **tiempo límite** no cuenta como "error de ejecución". | El objetivo EJECUTAR_CON_ERROR busca que el estudiante reconozca excepciones (por ejemplo `ArithmeticException`), no ciclos infinitos. |
| F5 | **Ejecutar** solo se habilita si la última compilación fue exitosa y el código no cambió desde entonces; al ejecutar, el servidor vuelve a compilar el mismo código. | Cumple el enunciado y evita ejecutar código distinto al compilado (guali-dev comparte la carpeta temporal entre usuarios). |
| F6 | **Pegar está deshabilitado** en el editor y no hay autocompletado. | El objetivo pedagógico es escribir el código a mano y enfrentar los errores del compilador sin asistencia. |
| F7 | Al **importar** una lección con un nombre que ya existe, se reemplaza por la nueva versión. Las lecciones inválidas se rechazan con su motivo y las válidas se importan. | Permite corregir lecciones subiendo el zip otra vez, sin borrar a mano. El administrador ve exactamente qué pasó con cada carpeta. |
| F8 | Las lecciones e imágenes se guardan en **MariaDB**, no en el disco del servidor. | El enunciado lo exige y facilita escalar y respaldar: los contenedores no guardan estado. |
| F9 | **Exportar** genera un zip con el mismo formato que la importación. | Sirve como respaldo y permite mover lecciones entre instalaciones. |
| F10 | La opción Configuración muestra importar/exportar/eliminar **solo con sesión de administrador**; además hay un panel con los módulos CRUD de administradores y estadísticas. | Separación de permisos pedida en "Tipos de usuarios" y "Administración del sistema". |
| F11 | **Estadísticas**: una visita por navegador cada 30 minutos; visitantes únicos = navegadores distintos en el intervalo. También se muestran lecciones abiertas, completadas y las más abiertas. | Responde "cuántos usuarios visitan la página en un intervalo" sin contar recargas como usuarios nuevos. |
| F12 | Ayuda integrada: guía de uso, tipos de objetivo, atajos de teclado y preguntas frecuentes. | El enunciado deja la ayuda a criterio; se cubren las dudas que surgen en la primera sesión. |
| F13 | El tema (claro/oscuro) y el tamaño de fuente se guardan por navegador en la base de datos. | Fase 2 pide persistencia; diferentes estudiantes tienen distintas preferencias. |

## No funcionales

| # | Decisión | Justificación |
|---|---|---|
| N1 | **Microservicios**: gateway, lecciones-service, admin-service y compilador-service, con una base de datos por servicio. | Requisito de arquitectura. El compilador ejecuta código de terceros: aislarlo en su propio servicio limita el daño posible. |
| N2 | **Spring Cloud Gateway** como punto único de entrada; el descubrimiento de servicios lo da el DNS de Docker. | Cloud-native sin servidor de descubrimiento adicional (Eureka): menos memoria en un VPS de 4 GB. En Kubernetes funcionaría igual con su DNS. |
| N3 | **Compilador aislado**: red interna sin Internet ni acceso a la base de datos, sin secretos, usuario sin privilegios, sistema de archivos de solo lectura, límites de CPU, memoria, procesos y tiempo (5 s), solicitudes atendidas de una en una. | guali-dev ejecuta código arbitrario de visitantes anónimos y usa una carpeta temporal y un nombre de clase (`app25`) compartidos. |
| N4 | **JWT firmado con RSA** en cookie HttpOnly, Secure y SameSite=Strict; solo admin-service tiene la llave privada. Sesión de 30 min que se renueva con la actividad. | El token no es accesible desde JavaScript (protección contra XSS). Los demás servicios validan sin poder emitir tokens. |
| N5 | **CSRF** con cookie `XSRF-TOKEN` + cabecera `X-XSRF-TOKEN` en toda petición que modifica datos. | Defensa contra CSRF además de SameSite. |
| N6 | Contraseñas con **BCrypt (costo 12)**, política de 12+ caracteres con mayúscula, minúscula y número, **bloqueo de 15 min tras 5 intentos fallidos** y mensaje genérico de error. | Protección contra fuerza bruta y enumeración de usuarios. |
| N7 | Importación de zip validada: tamaño máximo (10 MB), límite de entradas y de bytes descomprimidos (zip bomb), rechazo de rutas con `..` (zip slip), UTF-8 obligatorio y verificación de la firma binaria de las imágenes. **SVG no se acepta.** | Los archivos subidos son la principal superficie de ataque del panel de administración. SVG puede contener scripts. |
| N8 | Markdown convertido en el servidor: el HTML escrito dentro del markdown se escapa y el resultado pasa por el **sanitizador OWASP** con lista blanca. | Doble defensa contra XSS almacenado en el contenido de las lecciones. |
| N9 | **Content-Security-Policy** estricta (`script-src 'self'`, sin scripts ni estilos en línea), HSTS, X-Frame-Options, nosniff, Permissions-Policy. | Mitiga XSS, clickjacking y carga de recursos externos. Fuentes e íconos se sirven desde el propio servidor. |
| N10 | **Cloudflare** (WAF, protección DDoS, CDN seguro, TLS) delante de **nginx** como proxy inverso con seguridad integrada; el origen solo acepta conexiones de Cloudflare (Authenticated Origin Pulls). | Corresponde a los cuatro elementos de seguridad recomendados en el enunciado, con plan gratuito. |
| N11 | **Límite de solicitudes** en tres capas: Cloudflare, nginx y gateway (login, compilar e importar más estrictos). | Evita abuso del compilador y ataques de fuerza bruta. |
| N12 | **Flyway** para el esquema de base de datos (Hibernate no modifica tablas). | Esquema versionado y reproducible en cualquier servidor. |
| N13 | Contenedores con **usuario sin privilegios**, `cap_drop: ALL`, `no-new-privileges`, sistema de archivos de solo lectura y límites de memoria. | Reduce el impacto si un servicio es comprometido. |
| N14 | Secretos (contraseñas, llaves RSA, token interno) generados aleatoriamente en el servidor y **fuera del repositorio**. | Evita filtrar credenciales en el código entregado. |
| N15 | Llamadas entre servicios (`/interno/**`) protegidas con un **token interno** y no publicadas en el gateway. | Defensa en profundidad dentro de la red privada. |
| N16 | Frontend en **HTML, CSS y JavaScript** sin framework, organizado en **MVC** (modelo, vistas, controlador) y responsive. Backend con **Spring MVC** (controlador–servicio–repositorio). | Tecnologías exigidas; sin dependencias de terceros en el navegador. |
| N17 | Máquina virtual en **Microsoft Azure** (Ubuntu 24.04, Standard_B2s) con Docker Compose, en lugar de PaaS o Kubernetes administrado. MariaDB corre en un contenedor en la misma VM. | Control total de redes y límites para aislar el compilador; costo cubierto por el crédito de Azure for Students. Azure retiró su servicio administrado de MariaDB y el proyecto exige MariaDB. |
| N18 | El proyecto también se ejecuta **en una PC sin Docker**: un solo archivo (`configuracion.properties`) con la conexión a MariaDB, creación automática de las bases de datos, secretos generados la primera vez, Maven Wrapper e `iniciar.bat`. Las variables de entorno del servidor siempre tienen prioridad. | La entrega se revisa en otra computadora: debe funcionar solo con JDK 25 y MariaDB, sin herramientas adicionales ni pasos manuales. |
