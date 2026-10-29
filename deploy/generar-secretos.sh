#!/usr/bin/env bash
# Genera el archivo .env (contraseñas aleatorias) y el par de llaves RSA para firmar los JWT.
# No sobrescribe nada que ya exista. Uso: bash deploy/generar-secretos.sh
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
secretos="$raiz/deploy/secrets"
mkdir -p "$secretos"
chmod 700 "$secretos" 2>/dev/null || true

aleatorio() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c "${1:-32}"; }

if [[ ! -f "$secretos/jwt-privada.pem" ]]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$secretos/jwt-privada.pem" 2>/dev/null
  openssl pkey -in "$secretos/jwt-privada.pem" -pubout -out "$secretos/jwt-publica.pem"
  # Los contenedores corren con un usuario sin privilegios: deben poder leer las llaves.
  chmod 644 "$secretos/jwt-privada.pem" "$secretos/jwt-publica.pem"
  echo "Llaves RSA creadas en deploy/secrets/"
fi

if [[ ! -f "$raiz/.env" ]]; then
  cat > "$raiz/.env" <<EOF
# Generado por deploy/generar-secretos.sh — NO subir a git.
DB_CLAVE_ROOT=$(aleatorio 32)
DB_CLAVE_LECCIONES=$(aleatorio 32)
DB_CLAVE_ADMIN=$(aleatorio 32)
TOKEN_INTERNO=$(aleatorio 48)
TOKEN_COMPILADOR=$(aleatorio 48)
TOKEN_GATEWAY=$(aleatorio 48)
VISITAS_SAL=$(aleatorio 32)

# Primer administrador (se crea solo si no existe ninguno). Cámbiala después desde el panel.
ADMIN_USUARIO=admin
ADMIN_NOMBRE=Administrador
ADMIN_CLAVE=Guali$(aleatorio 14)7a

# true en producción (HTTPS con Cloudflare). false para pruebas locales en http://localhost
COOKIE_SEGURA=false
PUERTO_HTTP=8080
EOF
  chmod 600 "$raiz/.env" 2>/dev/null || true
  echo "Archivo .env creado. La contraseña inicial del administrador está en ADMIN_CLAVE."
fi

# Agrega a un .env existente los secretos que se hayan incorporado en versiones posteriores.
for clave in TOKEN_INTERNO TOKEN_COMPILADOR TOKEN_GATEWAY VISITAS_SAL; do
  if ! grep -q "^$clave=" "$raiz/.env"; then
    echo "$clave=$(aleatorio 48)" >> "$raiz/.env"
    echo "Agregado $clave a .env"
  fi
done
