# Despliegue en producción

Servidor: VPS Ubuntu 24.04 en DigitalOcean (crédito del GitHub Student Developer Pack) con Docker Compose.
Delante: Cloudflare (plan gratuito) como DNS, CDN, WAF, protección DDoS y TLS.

```
Usuario ─HTTPS─> Cloudflare ─HTTPS (mTLS)─> VPS :443 nginx ─> gateway ─> microservicios ─> MariaDB
```

## 1. Cuentas (las crea el dueño del proyecto)

1. **GitHub Student Developer Pack** → activar la oferta de DigitalOcean (crédito) y la de dominio gratis.
2. **Dominio**: registrar uno (por ejemplo `codificaconguali.me`).
3. **Cloudflare**: crear cuenta, "Add a site" con el dominio y cambiar los *nameservers* del registrador por los de Cloudflare. La propagación puede tardar de minutos a 24 h.

## 2. Servidor (Droplet)

- Imagen: Ubuntu 24.04 LTS · Plan: 2 vCPU / 4 GB RAM · Región: New York o San Francisco.
- Autenticación: **llave SSH** (no contraseña).

En el servidor, como root:

```bash
adduser guali && usermod -aG sudo guali
rsync --archive --chown=guali:guali ~/.ssh /home/guali
# SSH solo con llave y sin root
sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin no/; s/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart ssh
# Docker
curl -fsSL https://get.docker.com | sh
usermod -aG docker guali
# Actualizaciones de seguridad automáticas
apt-get install -y unattended-upgrades fail2ban
```

## 3. Firewall: solo Cloudflare puede llegar al puerto 443

En DigitalOcean → Networking → Firewalls (o con `ufw`):

- Entrada TCP 22: tu IP (o cualquiera, con llave SSH y fail2ban).
- Entrada TCP 443: solo los rangos de <https://www.cloudflare.com/ips/>.
- Todo lo demás cerrado. El puerto 8080 queda ligado a 127.0.0.1.

> Docker publica puertos saltándose `ufw`; por eso se recomienda el firewall de DigitalOcean (fuera del servidor).

## 4. Código y secretos

```bash
git clone <repositorio> guali && cd guali
bash deploy/generar-secretos.sh
nano .env      # COOKIE_SEGURA=true · PUERTO_HTTP=127.0.0.1:8080 · revisar ADMIN_USUARIO / ADMIN_NOMBRE
```

## 5. Certificados de Cloudflare

1. Cloudflare → SSL/TLS → Overview: modo **Full (strict)**.
2. SSL/TLS → Origin Server → **Create Certificate** (RSA, 15 años). Guardar en el servidor:
   - `deploy/nginx/tls/origen.pem` (certificado)
   - `deploy/nginx/tls/origen.key` (llave privada, `chmod 644` para que nginx sin privilegios la lea)
3. SSL/TLS → Origin Server → activar **Authenticated Origin Pulls** y guardar el certificado de Cloudflare como
   `deploy/nginx/tls/cloudflare-origin-pull-ca.pem` (<https://developers.cloudflare.com/ssl/static/authenticated_origin_pull_ca.pem>).
4. SSL/TLS → Edge Certificates: **Always Use HTTPS**, **Minimum TLS 1.2**, **HSTS**.

## 6. Levantar

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose ps          # todos en "healthy" o "running"
curl -s http://127.0.0.1:8080/api/lecciones | head -c 200
```

DNS en Cloudflare: registro `A` del dominio → IP del Droplet, con la **nube naranja (Proxied)** activada.

## 7. Seguridad en Cloudflare (plan gratuito)

- Security → WAF → **Managed rules**: activar el conjunto gratuito de Cloudflare.
- Security → WAF → **Rate limiting rules**: `/api/auth/login` (10 por minuto por IP) y `/api/codigo/` (40 por minuto por IP).
- Security → Bots: **Bot Fight Mode**.
- DDoS: activo por defecto en todos los planes.
- Caching → Cache Rules: no cachear `/api/*`.

## 8. Operación

- Logs: `docker compose logs -f lecciones admin gateway`
- Respaldo de la base de datos:
  ```bash
  docker compose exec mariadb sh -c 'mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --all-databases' | gzip > respaldo-$(date +%F).sql.gz
  ```
- Actualizar: `git pull && docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`
- Rangos de Cloudflare para nginx: `deploy/nginx/guali/cloudflare-ips.conf`.
