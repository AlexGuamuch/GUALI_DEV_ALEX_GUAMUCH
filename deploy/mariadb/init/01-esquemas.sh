#!/bin/bash
# Se ejecuta una sola vez, al crear el volumen de MariaDB.
# Una base de datos por microservicio, cada una con su propio usuario y permisos mínimos.
set -eo pipefail

mariadb -uroot -p"${MARIADB_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS guali_lecciones CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS guali_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'guali_lecciones'@'%' IDENTIFIED BY '${DB_CLAVE_LECCIONES}';
CREATE USER IF NOT EXISTS 'guali_admin'@'%' IDENTIFIED BY '${DB_CLAVE_ADMIN}';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON guali_lecciones.* TO 'guali_lecciones'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON guali_admin.* TO 'guali_admin'@'%';

FLUSH PRIVILEGES;
SQL
