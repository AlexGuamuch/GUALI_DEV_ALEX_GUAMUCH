-- Esquema del servicio de administración (base de datos guali_admin)

CREATE TABLE usuario (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    username          VARCHAR(30)  NOT NULL,
    nombre_completo   VARCHAR(80)  NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    activo            BOOLEAN      NOT NULL,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    bloqueado_hasta   DATETIME(6)  NULL,
    ultimo_acceso     DATETIME(6)  NULL,
    creado_en         DATETIME(6)  NOT NULL,
    actualizado_en    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Una visita por navegador cada 30 minutos. No se guarda la IP ni el identificador original:
-- "visitante" es un SHA-256 con sal del identificador anónimo del navegador.
CREATE TABLE visita (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    visitante  CHAR(64)    NOT NULL,
    fecha      DATE        NOT NULL,
    creada_en  DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_visita_fecha ON visita (fecha, visitante);
CREATE INDEX ix_visita_visitante ON visita (visitante, creada_en);
