# Codifica con Guali

Plataforma web para practicar Java con lecciones cortas: el estudiante escribe el código a mano, lo compila y lo ejecuta en el navegador. Proyecto del curso Desarrollo Web — Universidad Mariano Gálvez de Guatemala.

**Autor:** Alex Guamuch · carné 3190-21-2405

## Arquitectura

```
Navegador ─HTTPS─> Cloudflare (WAF, DDoS, CDN, TLS)
                     └─> nginx (proxy inverso, cabeceras de seguridad, límites, frontend estático)
                           └─> gateway (Spring Cloud Gateway: rutas, id anónimo, límite por IP)
                                 ├─> lecciones-service  (lecciones .zip, historial, preferencias, objetivos)
                                 │      ├─> MariaDB · guali_lecciones
                                 │      └─> compilador-service (guali-dev.jar, red aislada sin Internet)
                                 └─> admin-service      (login JWT, administradores, estadísticas)
                                        └─> MariaDB · guali_admin
```

| Módulo | Tecnología | Responsabilidad |
|---|---|---|
| `frontend/` | HTML, CSS, JavaScript (MVC en el cliente) | Interfaz del estudiante, login y panel de administración |
| `services/gateway` | Spring Cloud Gateway (WebFlux) | Punto único de entrada a la API |
| `services/lecciones-service` | Spring Boot, Spring MVC, REST, JPA, Flyway | Lecciones, historial, preferencias, compilar/ejecutar |
| `services/compilador-service` | Spring Boot + `guali-dev.jar` | Compila y ejecuta código Java de forma aislada |
| `services/admin-service` | Spring Boot, Spring Security, JPA | Autenticación, CRUD de administradores, estadísticas |
| `services/seguridad-comun` | Spring Security (resource server) | JWT en cookie, CSRF, errores JSON compartidos |

## Ejecutar en local

Requisitos: Docker Desktop. Para desarrollar fuera de Docker: JDK 25 y Maven 3.9.

```bash
bash deploy/generar-secretos.sh   # crea .env y las llaves RSA (no se suben a git)
docker compose up -d --build
```

Abrir <http://localhost:8080>. El administrador inicial es `admin` y su contraseña está en `ADMIN_CLAVE` dentro de `.env`.

Las lecciones de ejemplo se cargan solas si la base de datos está vacía. Para probar la importación, sube `lecciones-ejemplo.zip` desde Configuración → Importar compendio o desde el panel de administración.

## Formato de un compendio de lecciones

```
lecciones.zip
└── leccion-01-hola-mundo/
    ├── leccion.md     instrucciones en markdown
    ├── config.txt     una línea: objetivo=COMPILAR_CON_ERROR | COMPILAR_EXITOSO | EJECUTAR_CON_ERROR | EJECUTAR_EXITOSO
    └── imagen.png     opcional (PNG, JPG, GIF o WEBP)
```

Si una lección con el mismo nombre ya existe, se reemplaza por la nueva versión. Las lecciones con errores se rechazan con su motivo y las demás se importan.

## Pruebas

```bash
mvn test
```

Las pruebas del compilador usan `guali-dev.jar` real, así que Maven debe ejecutarse con JDK 25.
