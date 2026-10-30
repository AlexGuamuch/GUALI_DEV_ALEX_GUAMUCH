# Despliegue en Internet

Servidor: máquina virtual Ubuntu 24.04 en **Microsoft Azure** (crédito de Azure for Students) con Docker Compose.
Delante: Cloudflare (plan gratuito) como DNS, CDN, WAF, protección DDoS y TLS.

```
Usuario ─HTTPS─> Cloudflare ─HTTPS + certificado de cliente─> VM de Azure :443 nginx ─> gateway ─> microservicios ─> MariaDB
```

MariaDB corre en un contenedor dentro de la VM: Azure retiró su servicio administrado "Azure Database for MariaDB"
y el proyecto exige MariaDB.

Tiempo estimado: 1 a 2 horas, más la espera de los nameservers del dominio (de minutos a 24 h). Conviene hacer primero el paso 2.

---

## 1. Subir el código a GitHub (privado)

1. En GitHub: **New repository** → nombre `codifica-con-guali` → **Private** → sin README → Create.
2. En la carpeta del proyecto (PowerShell o Git Bash):
   ```bash
   git remote add origin https://github.com/TU-USUARIO/codifica-con-guali.git
   git push -u origin fase-2:main
   ```

## 2. Dominio y Cloudflare

1. Conseguir un dominio: el GitHub Student Developer Pack (<https://education.github.com/pack>) suele incluir uno gratis por un año (por ejemplo Namecheap `.me`); si no, un `.xyz` o `.site` cuesta 1 a 3 USD el primer año.
2. Crear cuenta en **Cloudflare** → **Add a domain** → plan **Free**.
3. Cloudflare muestra 2 *nameservers*: copiarlos en el registrador del dominio (Namecheap → Domain → Nameservers → Custom DNS).
4. Esperar el correo de Cloudflare "Your domain is now active".

## 3. Llave SSH (en tu PC)

```powershell
ssh-keygen -t ed25519 -C "guali"
type $env:USERPROFILE\.ssh\id_ed25519.pub
```

Copiar la línea que empieza con `ssh-ed25519`.

## 4. Crear la máquina virtual en Azure

<https://portal.azure.com> → **Virtual machines → Create → Azure virtual machine**.

**Basics**

| Campo | Valor |
|---|---|
| Subscription | Azure for Students |
| Resource group | Create new → `guali-rg` |
| Virtual machine name | `guali` |
| Region | **East US 2**, **South Central US** o **Central US** (las suscripciones de estudiante solo permiten algunas regiones; si una da error, prueba otra) |
| Availability options | No infrastructure redundancy required |
| Security type | Standard |
| Image | **Ubuntu Server 24.04 LTS - x64 Gen2** |
| Size | **Standard_B2s** (2 vCPU, 4 GiB). Si no está disponible: **Standard_B2als_v2** u otro de 2 vCPU y 4 GiB |
| Authentication type | **SSH public key** |
| Username | `guali` |
| SSH public key source | Use existing public key → pegar la llave del paso 3 |
| Public inbound ports | Allow selected ports → **SSH (22)** |

**Disks:** OS disk type **Standard SSD** (30 GiB es suficiente).
**Networking:** dejar la red y la IP pública que propone (la IP Standard es fija).
**Management:** confirmar que **Auto-shutdown** esté **desactivado** (la aplicación debe estar siempre en línea).

**Review + create → Create.** Al terminar, anotar la **Public IP address** de la VM.

> Costo aproximado: 35 USD al mes (VM B2s + disco + IP). El crédito de Azure for Students alcanza para varios meses y,
> si se agota, la suscripción se suspende sin cobrar a una tarjeta. Revisa el precio actual en la calculadora de Azure.

**Alerta de gasto (recomendado):** Cost Management → **Budgets → Add** → 25 USD al mes con aviso por correo.

## 5. Abrir el puerto 443 (grupo de seguridad de red)

VM `guali` → **Networking → Network settings → Create port rule → Inbound port rule**:

| Campo | Valor |
|---|---|
| Source | Any |
| Destination port ranges | 443 |
| Protocol | TCP |
| Action | Allow |
| Name | `Permitir-HTTPS` |

Opcional pero recomendado: editar la regla **SSH** y cambiar Source a **My IP address**.

Todo lo demás queda cerrado. Aunque el 443 esté abierto, nginx solo acepta conexiones que traen el certificado de
Cloudflare (paso 9). Docker publica puertos saltándose `ufw`; por eso se usa el grupo de seguridad de red de Azure,
que filtra fuera de la VM.

## 6. Preparar el servidor

```bash
ssh guali@IP_DE_LA_VM
```

Azure ya creó el usuario `guali` con permisos de administrador, sin contraseña y sin acceso como root.

```bash
# SSH solo con llave (Azure ya lo deja así; se refuerza por si acaso)
sudo sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin no/; s/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl restart ssh

# Actualizaciones automáticas de seguridad y bloqueo de ataques a SSH
sudo apt-get update && sudo apt-get -y upgrade
sudo apt-get install -y unattended-upgrades fail2ban git

# 2 GB de swap (la primera compilación de las imágenes usa bastante memoria)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker guali
exit
```

Volver a entrar con `ssh guali@IP_DE_LA_VM` para que el permiso de Docker tenga efecto.

## 7. Descargar el código en el servidor

```bash
ssh guali@IP_DE_LA_VM
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
| A | `@` | IP pública de la VM | **Proxied** (nube naranja) |
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
| Al crear la VM: `RequestDisallowedByAzure` o "size not available" | La suscripción de estudiante no permite esa región o tamaño: elegir otra región (East US 2, South Central US, Central US) u otro tamaño de 2 vCPU y 4 GiB. |
| `ssh` no conecta | Revisar la regla SSH del grupo de seguridad (si es "My IP address" y cambió tu IP, actualizarla). |
