# Diagramas

## Diagrama entidad-relación

Cada microservicio tiene su propia base de datos en MariaDB (patrón *database per service*). No hay llaves foráneas entre bases: lecciones-service y admin-service se comunican por REST.

```mermaid
erDiagram
    %% Base de datos guali_lecciones (lecciones-service)
    LECCION ||--o{ RECURSO_LECCION : "tiene imágenes"
    LECCION |o--o{ INTENTO : "se registra en"

    LECCION {
        BIGINT id PK
        VARCHAR nombre UK "nombre de la carpeta = título"
        VARCHAR objetivo "COMPILAR_CON_ERROR | COMPILAR_EXITOSO | EJECUTAR_CON_ERROR | EJECUTAR_EXITOSO"
        VARCHAR resumen
        MEDIUMTEXT contenido_md "leccion.md original"
        MEDIUMTEXT contenido_html "HTML sanitizado"
        DATETIME creada_en
        DATETIME actualizada_en
    }
    RECURSO_LECCION {
        BIGINT id PK
        BIGINT leccion_id FK "ON DELETE CASCADE"
        VARCHAR nombre_archivo "UK con leccion_id"
        VARCHAR tipo_contenido "image/png, image/jpeg..."
        INT tamano
        LONGBLOB datos
    }
    INTENTO {
        BIGINT id PK
        CHAR cliente_id "id anónimo del navegador"
        BIGINT leccion_id FK "ON DELETE SET NULL"
        VARCHAR leccion_nombre "se conserva si se borra la lección"
        VARCHAR objetivo
        DATETIME fecha_inicio
        DATETIME fecha_fin "completó, cerró o abandonó"
        VARCHAR estado "PENDIENTE | COMPLETADA"
    }
    PREFERENCIA {
        CHAR cliente_id PK
        VARCHAR tema "light | dark"
        SMALLINT tamano_fuente "12 a 24"
        DATETIME actualizada_en
    }

    %% Base de datos guali_admin (admin-service)
    USUARIO {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR nombre_completo
        VARCHAR password_hash "BCrypt"
        BOOLEAN activo
        INT intentos_fallidos
        DATETIME bloqueado_hasta
        DATETIME ultimo_acceso
        DATETIME creado_en
        DATETIME actualizado_en
    }
    VISITA {
        BIGINT id PK
        CHAR visitante "SHA-256 con sal del id anónimo"
        DATE fecha "día en hora de Guatemala"
        DATETIME creada_en
    }
```

`INTENTO.cliente_id`, `PREFERENCIA.cliente_id` y el origen de `VISITA.visitante` son el mismo identificador anónimo del navegador, asignado por el gateway en la cookie `guali_cliente`.

## Diagrama de despliegue

```mermaid
flowchart TB
    subgraph Cliente["Dispositivo del usuario"]
        B["Navegador<br/>HTML · CSS · JavaScript"]
    end

    subgraph CF["Cloudflare (plan gratuito)"]
        CDN["CDN + TLS<br/>WAF · protección DDoS<br/>oculta la IP del servidor"]
    end

    subgraph VPS["Google Cloud Compute Engine · e2-standard-2 · Ubuntu 24.04 · Docker Compose"]
        FW["Firewall de la VPC: solo 443 y SSH<br/>443 exige certificado de Cloudflare (mTLS)<br/>SSH solo con llave"]
        subgraph borde["red: borde"]
            NG["nginx 1.29<br/>proxy inverso · cabeceras de seguridad<br/>límites por IP · frontend estático"]
            GW["gateway<br/>Spring Cloud Gateway<br/>rutas · id anónimo · límite por IP"]
        end
        subgraph servicios["red interna: servicios"]
            LS["lecciones-service<br/>Spring Boot · JPA · Flyway"]
            AS["admin-service<br/>Spring Boot · Security · JPA"]
        end
        subgraph compilador["red interna: compilador (sin Internet)"]
            CS["compilador-service<br/>guali-dev.jar · JDK 25<br/>solo lectura · sin privilegios · límites CPU/RAM"]
        end
        subgraph datos["red interna: datos"]
            DB[("MariaDB 11.8<br/>guali_lecciones · guali_admin")]
        end
    end

    B -- "HTTPS" --> CDN
    CDN -- "HTTPS (origen)" --> FW --> NG
    NG -- "/api/**" --> GW
    GW --> LS
    GW --> AS
    LS -- "REST + token interno" --> CS
    AS -- "REST + token interno" --> LS
    LS --> DB
    AS --> DB
```
