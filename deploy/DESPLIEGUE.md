# Despliegue en Internet

Servidor: VPS Ubuntu 24.04 en DigitalOcean (crédito del GitHub Student Developer Pack) con Docker Compose.
Delante: Cloudflare (plan gratuito) como DNS, CDN, WAF, protección DDoS y TLS.

```
Usuario ─HTTPS─> Cloudflare ─HTTPS + certificado de cliente─> VPS :443 nginx ─> gateway ─> microservicios ─> MariaDB
```

Tiempo estimado: 1 a 2 horas, más la espera de los nameservers del dominio (de minutos a 24 h). Conviene hacer primero el paso 2.

---

## 1. Subir el código a GitHub (privado)

1. En GitHub: **New repository** → nombre `codifica-con-guali` → **Private** → sin README → Create.
2. En la carpeta del proyecto (PowerShell o Git Bash):
   ```bash
   git remote add origin https://github.com/TU-USUARIO/codifica-con-guali.git
   git push -u origin fase-2:main
   ```

## 2. Cuentas y dominio

1. <https://education.github.com/pack> → activar la oferta de **DigitalOcean** (crédito) y la de un **dominio gratis** (por ejemplo Namecheap `.me`).
2. Crear cuenta en **Cloudflare** → **Add a domain** → plan **Free**.
3. Cloudflare muestra 2 *nameservers*: copiarlos en el registrador del dominio (Namecheap → Domain → Nameservers → Custom DNS).
4. Esperar el correo de Cloudflare "Your domain is now active".

## 3. Llave SSH (en tu PC)

```powershell
ssh-keygen -t ed25519 -C "guali"
type $env:USERPROFILE\.ssh\id_ed25519.pub
```

Copiar la línea que empieza con `ssh-ed25519`.

## 4. Crear el servidor (Droplet)

DigitalOcean → **Create → Droplets**:

- Región: **New York** o **San Francisco**.
- Imagen: **Ubuntu 24.04 LTS**.
- Plan: **Basic / Regular · 2 vCPU · 4 GB RAM**.
- Autenticación: **SSH Key** → pegar la llave del paso 3.
- Nombre: `guali`.

Anotar la **IP pública**.

## 5. Firewall de DigitalOcean

Networking → **Firewalls → Create Firewall** → aplicarlo al Droplet `guali`.

| Entrada | Puerto | Origen |
|---|---|---|
| SSH | 22 | All IPv4/IPv6 (o solo tu IP) |
| HTTPS | 443 | All IPv4/IPv6 |

Todo lo demás cerrado. Aunque el 443 esté abierto, nginx solo acepta conexiones que traen el certificado de Cloudflare (paso 9).
Docker publica puertos saltándose `ufw`; por eso se usa el firewall de DigitalOcean, que está fuera del servidor.

## 6. Preparar el servidor

```bash
ssh root@IP_DEL_SERVIDOR
```

```bash
# Usuario sin privilegios con tu llave SSH
adduser --disabled-password --gecos "" guali
usermod -aG sudo guali
echo "guali ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/guali
rsync --archive --chown=guali:guali ~/.ssh /home/guali

# SSH solo con llave y sin root
sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin no/; s/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart ssh

# Actualizaciones automáticas de seguridad y bloqueo de ataques a SSH
apt-get update && apt-get -y upgrade
apt-get install -y unattended-upgrades fail2ban git

# 2 GB de swap (la primera compilación de las imágenes usa bastante memoria)
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Docker
curl -fsSL https://get.docker.com | sh
usermod -aG docker guali
exit
```

## 7. Descargar el código en el servidor

```bash
ssh guali@IP_DEL_SERVIDOR
ssh-keygen -t ed25519 -C "servidor-guali" -N "" -f ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
```

En GitHub: repositorio → **Settings → Deploy keys → Add deploy key** → pegar la llave (solo lectura). Luego:

```bash
git clone git@github.com:TU-USUARIO/codifica-con-guali.git guali
cd guali
```

## 8. Secretos del servidor

```bash
bash deploy/generar-secretos.sh
nano .env
```

En `.env` cambiar:

```
COOKIE_SEGURA=true
PUERTO_HTTP=127.0.0.1:8080
ADMIN_USUARIO=admin
ADMIN_NOMBRE=Alex Guamuch
```

`ADMIN_CLAVE` ya trae una contraseña aleatoria segura (verla con `grep ADMIN_CLAVE .env`). Guardar con Ctrl+O, Enter, Ctrl+X.

## 9. Certificados de Cloudflare

En Cloudflare, con el dominio seleccionado:

1. **SSL/TLS → Overview**: modo **Full (strict)**.
2. **SSL/TLS → Origin Server → Create Certificate** (RSA, 15 años, con el dominio y `*.dominio`). En el servidor:
   ```bash
   nano deploy/nginx/tls/origen.pem    # pegar "Origin Certificate"
   nano deploy/nginx/tls/origen.key    # pegar "Private Key"
   ```
3. **SSL/TLS → Origin Server → Authenticated Origin Pulls**: activar. En el servidor:
   ```bash
   curl -fsSL https://developers.cloudflare.com/ssl/static/authenticated_origin_pull_ca.pem \
     -o deploy/nginx/tls/cloudflare-origin-pull-ca.pem
   chmod 644 deploy/nginx/tls/*
   ```
4. **SSL/TLS → Edge Certificates**: activar **Always Use HTTPS**, **Minimum TLS Version 1.2** y **HSTS**.

## 10. Levantar la aplicación

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose ps
```

La primera vez tarda de 10 a 15 minutos (descarga imágenes y compila). Esperar a que `lecciones`, `admin`, `compilador` y `mariadb` digan `healthy`. Comprobación interna:

```bash
curl -s http://127.0.0.1:8080/api/lecciones | head -c 200
```

## 11. Apuntar el dominio al servidor

Cloudflare → **DNS → Records → Add record**:

| Tipo | Nombre | Contenido | Proxy |
|---|---|---|---|
| A | `@` | IP del servidor | **Proxied** (nube naranja) |
| CNAME | `www` | tu dominio | **Proxied** |

Abrir `https://tu-dominio` y entrar a `https://tu-dominio/login.html` con `ADMIN_USUARIO` / `ADMIN_CLAVE`.

## 12. Verificación final

Desde el servidor:

```bash
bash deploy/prueba-humo.sh https://tu-dominio
```

Debe terminar con `21 correctas, 0 con falla`. Hazla antes del paso 13: Bot Fight Mode puede bloquear a `curl`.

## 13. Seguridad en Cloudflare (plan gratuito)

- **Security → WAF → Managed rules**: activar el conjunto gratuito de Cloudflare.
- **Security → WAF → Rate limiting rules** (1 regla gratis): URI path contiene `/api/` → 100 solicitudes por 10 segundos por IP → Block.
- **Security → Bots**: activar **Bot Fight Mode**.
- **Caching → Cache Rules**: regla "URI path starts with `/api/`" → **Bypass cache**.
- La protección contra DDoS está activa por defecto.

---

## Operación

- Ver registros: `docker compose logs -f lecciones admin gateway`
- Actualizar después de un `git push`:
  ```bash
  cd ~/guali && git pull
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
  ```
- Respaldo de la base de datos:
  ```bash
  docker compose exec mariadb sh -c 'mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --all-databases' | gzip > respaldo-$(date +%F).sql.gz
  ```
- Rangos de Cloudflare para la IP real de los visitantes: `bash deploy/actualizar-ips-cloudflare.sh`.

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| Error 526 en el navegador | Falta o está mal `origen.pem` / `origen.key`, o el modo SSL no es Full (strict). |
| Error 525 / 400 "No required SSL certificate was sent" | Authenticated Origin Pulls no está activado en Cloudflare. |
| Error 521 | nginx no está corriendo: `docker compose logs nginx`. |
| Error 502 | El gateway o un servicio aún arranca: `docker compose ps`. |
| No llega la página | Los nameservers todavía no cambian, o el registro DNS no está "Proxied". |
