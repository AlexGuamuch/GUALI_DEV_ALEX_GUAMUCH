-- Esquema del servicio de lecciones (base de datos guali_lecciones)

CREATE TABLE leccion (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100) NOT NULL,
    objetivo       VARCHAR(30)  NOT NULL,
    resumen        VARCHAR(300) NOT NULL,
    contenido_md   MEDIUMTEXT   NOT NULL,
    contenido_html MEDIUMTEXT   NOT NULL,
    creada_en      DATETIME(6)  NOT NULL,
    actualizada_en DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leccion_nombre UNIQUE (nombre),
    CONSTRAINT ck_leccion_objetivo CHECK (objetivo IN
        ('COMPILAR_CON_ERROR', 'COMPILAR_EXITOSO', 'EJECUTAR_CON_ERROR', 'EJECUTAR_EXITOSO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recurso_leccion (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    leccion_id     BIGINT       NOT NULL,
    nombre_archivo VARCHAR(150) NOT NULL,
    tipo_contenido VARCHAR(40)  NOT NULL,
    tamano         INT          NOT NULL,
    datos          LONGBLOB     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recurso_leccion UNIQUE (leccion_id, nombre_archivo),
    CONSTRAINT fk_recurso_leccion FOREIGN KEY (leccion_id) REFERENCES leccion (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Historial: cada vez que un navegador abre una lección se registra un intento.
-- Se guarda el nombre de la lección para conservar el historial aunque la lección se elimine.
CREATE TABLE intento (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    cliente_id     CHAR(36)     NOT NULL,
    leccion_id     BIGINT       NULL,
    leccion_nombre VARCHAR(100) NOT NULL,
    objetivo       VARCHAR(30)  NOT NULL,
    fecha_inicio   DATETIME(6)  NOT NULL,
    fecha_fin      DATETIME(6)  NULL,
    estado         VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_intento_leccion FOREIGN KEY (leccion_id) REFERENCES leccion (id) ON DELETE SET NULL,
    CONSTRAINT ck_intento_estado CHECK (estado IN ('PENDIENTE', 'COMPLETADA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX ix_intento_cliente ON intento (cliente_id, fecha_inicio);
CREATE INDEX ix_intento_fecha ON intento (fecha_inicio);

-- Preferencias de apariencia por navegador (tema y tamaño de fuente).
CREATE TABLE preferencia (
    cliente_id     CHAR(36)    NOT NULL,
    tema           VARCHAR(10) NOT NULL,
    tamano_fuente  SMALLINT    NOT NULL,
    actualizada_en DATETIME(6) NOT NULL,
    PRIMARY KEY (cliente_id),
    CONSTRAINT ck_preferencia_tema CHECK (tema IN ('light', 'dark')),
    CONSTRAINT ck_preferencia_fuente CHECK (tamano_fuente BETWEEN 12 AND 24)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
