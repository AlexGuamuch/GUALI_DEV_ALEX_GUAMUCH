#!/usr/bin/env bash
# Regenera deploy/nginx/guali/cloudflare-ips.conf con los rangos publicados por Cloudflare.
set -euo pipefail

destino="$(cd "$(dirname "$0")" && pwd)/nginx/guali/cloudflare-ips.conf"
{
  echo "# Rangos de Cloudflare (https://www.cloudflare.com/ips/). Solo desde estas IPs se acepta CF-Connecting-IP."
  echo "# Generado por deploy/actualizar-ips-cloudflare.sh el $(date +%F)"
  for version in v4 v6; do
    curl -fsS "https://www.cloudflare.com/ips-$version" | while read -r rango; do
      [[ -n "$rango" ]] && echo "set_real_ip_from $rango;"
    done
  done
} > "$destino.tmp"
mv "$destino.tmp" "$destino"
echo "Actualizado $destino. Recarga nginx: docker compose exec nginx nginx -s reload"
