#!/usr/bin/env bash
# Prueba de humo de la API completa a través de nginx → gateway → microservicios.
# Uso: bash deploy/prueba-humo.sh [url-base]   (por defecto http://localhost:8080)
set -euo pipefail

BASE="${1:-http://localhost:8080}"
raiz="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
jar="$tmp/cookies.txt"
touch "$jar"
ok=0; fallas=0

paso() { printf '%-58s' "$1"; }
bien() { echo "OK"; ok=$((ok + 1)); }
mal()  { echo "FALLA: $1"; fallas=$((fallas + 1)); }
xsrf() { awk '$6 == "XSRF-TOKEN" {print $7}' "$jar" | tail -1; }
pedir() { # metodo ruta [cuerpo-json]
  local args=(-s -b "$jar" -c "$jar" -X "$1" -H "Accept: application/json" -H "X-XSRF-TOKEN: $(xsrf)")
  [[ $# -ge 3 ]] && args+=(-H "Content-Type: application/json" --data "$3")
  curl "${args[@]}" -w '\n%{http_code}' "$BASE$2"
}
codigo() { tail -n1 <<<"$1"; }
cuerpo() { sed '$d' <<<"$1"; }

paso "Sesión inicial (cookies XSRF y guali_cliente)"
r=$(pedir GET /api/auth/sesion)
[[ $(codigo "$r") == 200 && -n "$(xsrf)" ]] && grep -q guali_cliente "$jar" && bien || mal "$(codigo "$r")"

paso "Listar lecciones"
r=$(pedir GET /api/lecciones)
id=$(cuerpo "$r" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[[ $(codigo "$r") == 200 && -n "$id" ]] && bien || mal "$(codigo "$r")"

paso "Detalle de lección con HTML sanitizado"
r=$(pedir GET "/api/lecciones/$id")
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q '"html"' && bien || mal "$(codigo "$r")"

paso "POST sin token CSRF es rechazado (403)"
c=$(curl -s -o /dev/null -w '%{http_code}' -b "$jar" -X POST -H "Content-Type: application/json" --data "{\"leccionId\":$id}" "$BASE/api/intentos")
[[ $c == 403 ]] && bien || mal "$c"

paso "Iniciar intento (historial)"
r=$(pedir POST /api/intentos "{\"leccionId\":$id}")
intento=$(cuerpo "$r" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[[ $(codigo "$r") == 200 && -n "$intento" ]] && bien || mal "$(codigo "$r")"

paso "Compilar con error"
r=$(pedir POST /api/codigo/compilar "{\"codigo\":\"void main() {\\n IO.println(\\\"x\\\")\\n}\",\"intentoId\":$intento}")
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q '"exitosa":false' && bien || mal "$(codigo "$r") $(cuerpo "$r" | head -c 200)"

paso "Ejecutar Hola Mundo (objetivo cumplido)"
r=$(pedir POST /api/codigo/ejecutar "{\"codigo\":\"void main() {\\n IO.println(\\\"Hola, Mundo\\\");\\n}\",\"intentoId\":$intento}")
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q 'Hola, Mundo' && bien || mal "$(codigo "$r") $(cuerpo "$r" | head -c 200)"

paso "Historial del navegador"
r=$(pedir GET /api/intentos)
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q "\"id\":$intento" && bien || mal "$(codigo "$r")"

paso "Guardar preferencias"
r=$(pedir PUT /api/preferencias '{"tema":"dark","tamanoFuente":16}')
[[ $(codigo "$r") == 200 ]] && bien || mal "$(codigo "$r")"

paso "Registrar visita"
r=$(pedir POST /api/visitas)
[[ $(codigo "$r") == 204 ]] && bien || mal "$(codigo "$r")"

paso "Panel sin sesión es rechazado (401)"
r=$(pedir GET /api/admin/usuarios)
[[ $(codigo "$r") == 401 ]] && bien || mal "$(codigo "$r")"

paso "Rutas internas no publicadas (404)"
c=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/interno/estadisticas?desde=2026-01-01&hasta=2026-01-02")
[[ $c == 404 ]] && bien || mal "$c"

# Credenciales candidatas: variables GUALI_ADMIN_*, configuracion.properties (PC sin Docker) y .env (Docker).
candidatos=()
[[ -n "${GUALI_ADMIN_USUARIO:-}" ]] && candidatos+=("$GUALI_ADMIN_USUARIO:$GUALI_ADMIN_CLAVE")
if [[ -f "$raiz/configuracion.properties" ]]; then
  u=$(grep '^admin.usuario=' "$raiz/configuracion.properties" | cut -d= -f2- | tr -d '')
  c=$(grep '^admin.clave=' "$raiz/configuracion.properties" | cut -d= -f2- | tr -d '')
  [[ -n "$u" ]] && candidatos+=("$u:$c")
fi
if [[ -f "$raiz/.env" ]]; then
  u=$(grep '^ADMIN_USUARIO=' "$raiz/.env" | cut -d= -f2- | tr -d '')
  c=$(grep '^ADMIN_CLAVE=' "$raiz/.env" | cut -d= -f2- | tr -d '')
  [[ -n "$u" ]] && candidatos+=("$u:$c")
fi
usuario="${candidatos[0]%%:*}"

paso "Login con contraseña incorrecta (401)"
r=$(pedir POST /api/auth/login "{\"username\":\"$usuario\",\"password\":\"incorrecta\"}")
[[ $(codigo "$r") == 401 ]] && bien || mal "$(codigo "$r")"

paso "Login de administrador (cookie HttpOnly)"
logueado=no
for cand in "${candidatos[@]}"; do
  r=$(pedir POST /api/auth/login "{\"username\":\"${cand%%:*}\",\"password\":\"${cand#*:}\"}")
  if [[ $(codigo "$r") == 200 ]] && grep -q GUALI_SESION "$jar"; then logueado=si; break; fi
done
[[ $logueado == si ]] && bien || mal "$(codigo "$r")"

paso "Listar administradores"
r=$(pedir GET /api/admin/usuarios)
[[ $(codigo "$r") == 200 ]] && bien || mal "$(codigo "$r")"

paso "Importar lecciones-ejemplo.zip"
r=$(curl -s -b "$jar" -c "$jar" -H "X-XSRF-TOKEN: $(xsrf)" -F "archivo=@$raiz/lecciones-ejemplo.zip" -w '\n%{http_code}' "$BASE/api/admin/lecciones/importar")
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q '"actualizadas"' && bien || mal "$(codigo "$r") $(cuerpo "$r" | head -c 200)"

paso "Importar un archivo que no es zip (400)"
echo "no soy un zip" > "$tmp/falso.zip"
c=$(curl -s -o /dev/null -b "$jar" -c "$jar" -H "X-XSRF-TOKEN: $(xsrf)" -F "archivo=@$tmp/falso.zip" -w '%{http_code}' "$BASE/api/admin/lecciones/importar")
[[ $c == 400 ]] && bien || mal "$c"

paso "Exportar lecciones (.zip)"
curl -s -b "$jar" -o "$tmp/export.zip" "$BASE/api/admin/lecciones/exportar"
[[ "$(head -c 2 "$tmp/export.zip")" == "PK" ]] && bien || mal "no es zip"

paso "Estadísticas de uso"
r=$(pedir GET /api/admin/estadisticas)
[[ $(codigo "$r") == 200 ]] && cuerpo "$r" | grep -q '"totales"' && bien || mal "$(codigo "$r")"

paso "Cerrar sesión"
r=$(pedir POST /api/auth/logout)
[[ $(codigo "$r") == 200 ]] && bien || mal "$(codigo "$r")"

paso "Cabeceras de seguridad (CSP, nosniff, X-Frame-Options)"
h=$(curl -s -D - -o /dev/null "$BASE/")
grep -qi 'content-security-policy' <<<"$h" && grep -qi 'x-content-type-options: nosniff' <<<"$h" && grep -qi 'x-frame-options: deny' <<<"$h" && bien || mal "faltan cabeceras"

echo
echo "Resultado: $ok correctas, $fallas con falla"
[[ $fallas -eq 0 ]]
