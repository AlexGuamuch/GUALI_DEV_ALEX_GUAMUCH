# Codifica con Guali

Plataforma web para practicar Java con lecciones cortas: el estudiante escribe el código a mano, lo compila y lo ejecuta en el navegador. Proyecto del curso Desarrollo Web — Universidad Mariano Gálvez de Guatemala.

**Autor:** Alex Guamuch · carné 3190-21-2405

## Ejecutar en una PC (Windows)

### Requisitos

- **JDK 25** (con `JAVA_HOME` apuntando a él).
- **MariaDB** instalado y en ejecución.
- No hace falta instalar Maven: el proyecto trae Maven Wrapper (`mvnw`). La primera vez descarga Maven y las dependencias, así que requiere Internet.

### Pasos

1. Abrir `configuracion.properties` (en esta carpeta) y revisar la conexión a MariaDB:
   ```properties
   db.servidor=localhost
   db.puerto=3306
   db.usuario=root
   db.clave=          # contraseña de root de MariaDB
   ```
   Las bases `guali_lecciones` y `guali_admin` y sus tablas **se crean solas** al arrancar. También se cargan 5 lecciones de ejemplo.
2. Doble clic en **`iniciar.bat`**. Compila el proyecto, abre una ventana por cada microservicio y, cuando todo está listo, abre <http://localhost:8080>.
3. Administración: <http://localhost:8080/login.html> con usuario **`admin`** y contraseña **`GualiDev2026!`**. Se pueden cambiar en `configuracion.properties` antes del primer arranque y después desde el panel.
4. Para detener todo: **`detener.bat`** o cerrar las ventanas "Guali - …".

**Desde IntelliJ:** abrir el `pom.xml` de esta carpeta como proyecto, elegir JDK 25 en Project Structure y ejecutar la configuración **Guali · todos los servicios**. Es la misma configuración de `configuracion.properties`.

### Si algo falla

| Síntoma | Solución |
|---|---|
| `Access denied for user 'root'` en la ventana de lecciones o admin | Corregir `db.usuario` y `db.clave` en `configuracion.properties`. |
| `Could not connect` / `Connection refused` a MariaDB | Verificar que el servicio MariaDB esté iniciado y el puerto (`db.puerto`). |
| "El puerto 8080 ya está en uso" | Cerrar la aplicación que lo usa (por ejemplo, Docker) o ejecutar `detener.bat`. |
| "No se encuentra el servidor de Guali" | Abrir la aplicación en <http://localhost:8080>, no desde la vista previa de IntelliJ ni como archivo. |
| "Demasiados intentos fallidos" | La cuenta se bloquea 15 minutos tras 5 contraseñas incorrectas. Esperar, o desbloquear en MariaDB: `UPDATE guali_admin.usuario SET intentos_fallidos=0, bloqueado_hasta=NULL;` |
| "Se necesita JDK 25" | Instalar JDK 25 y configurar `JAVA_HOME`. |

Los secretos de la instalación local (tokens entre servicios y llaves RSA del inicio de sesión) se generan solos la primera vez en `%USERPROFILE%\.guali`.

## Arquitectura

```
Navegador ─HTTPS─> Cloudflare (WAF, DDoS, CDN, TLS)           ← solo en el servidor
                     └─> nginx (proxy inverso, cabeceras, límites)
                           └─> gateway :8080 (Spring Cloud Gateway: rutas, id anónimo, límite por IP)
                                 ├─> lecciones-service :8081  (lecciones .zip, historial, preferencias, objetivos)
                                 │      ├─> MariaDB · guali_lecciones
                                 │      └─> compilador-service :8083 (guali-dev.jar)
                                 └─> admin-service :8082      (login JWT, administradores, estadísticas)
                                        └─> MariaDB · guali_admin
```

En una PC el gateway también sirve el frontend; en el servidor lo hace nginx.

| Módulo | Tecnología | Responsabilidad |
|---|---|---|
| `frontend/` | HTML, CSS, JavaScript (MVC en el cliente) | Interfaz del estudiante, login y panel de administración |
| `services/gateway` | Spring Cloud Gateway (WebFlux) | Punto único de entrada a la API |
| `services/lecciones-service` | Spring Boot, Spring MVC, REST, JPA, Flyway | Lecciones, historial, preferencias, compilar/ejecutar |
| `services/compilador-service` | Spring Boot + `guali-dev.jar` | Compila y ejecuta código Java |
| `services/admin-service` | Spring Boot, Spring Security, JPA | Autenticación, CRUD de administradores, estadísticas |
| `services/seguridad-comun` | Spring Security (resource server) | JWT en cookie, CSRF, errores JSON compartidos |
| `services/configuracion-local` | Spring Boot | Lee `configuracion.properties` y genera los secretos en una PC |

## Formato de un compendio de lecciones

```
lecciones.zip
└── leccion-01-hola-mundo/
    ├── leccion.md     instrucciones en markdown
    ├── config.txt     una línea: objetivo=COMPILAR_CON_ERROR | COMPILAR_EXITOSO | EJECUTAR_CON_ERROR | EJECUTAR_EXITOSO
    └── imagen.png     opcional (PNG, JPG, GIF o WEBP)
```

Se importa desde Configuración → Importar compendio (con sesión de administrador) o desde el panel. Si una lección con el mismo nombre ya existe, se reemplaza por la nueva versión. Las lecciones con errores se rechazan con su motivo y las demás se importan. `lecciones-ejemplo.zip` sirve para probarlo.

## Servidor en Internet (Docker)

En el servidor la aplicación corre con Docker Compose, con aislamiento adicional del compilador (red sin Internet, sistema de archivos de solo lectura, límites de CPU y memoria) y nginx detrás de Cloudflare. Ver [`deploy/DESPLIEGUE.md`](deploy/DESPLIEGUE.md).

```bash
bash deploy/generar-secretos.sh   # crea .env y las llaves RSA del servidor (no se suben a git)
docker compose up -d --build
```

## Pruebas

```bash
mvnw.cmd test                        # pruebas unitarias (Windows; en Linux/macOS: ./mvnw test)
bash deploy/prueba-humo.sh           # recorre toda la API con la aplicación en marcha
```
