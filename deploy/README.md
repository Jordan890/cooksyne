# Cart & Cook — Deployment Guide

Self-hosted deployment of Cart & Cook using Docker Compose with automatic HTTPS via Tailscale.

## Architecture

```
Browser (any device on your tailnet)
  │
  │  HTTPS (automatic Tailscale TLS certificate)
  ▼
┌─────────────────────────────────────────┐
│  Caddy (reverse proxy)                  │
│  https://<hostname>.<tailnet>.ts.net    │
│                                         │
│  /           → UI        (nginx:80)     │
│  /auth/*     → Keycloak  (8080)         │
│  /api/*      → Backend   (8081)         │
└─────────┬───────────┬───────────────────┘
          │           │
    ┌─────┘     ┌─────┘
    ▼           ▼
┌────────┐ ┌────────┐ ┌────────────┐
│  UI    │ │Backend │ │ Keycloak   │
│ nginx  │ │ :8081  │ │   :8080    │
└────────┘ └───┬────┘ └─────┬──────┘
               │             │
               ▼             ▼
          ┌──────────────────────┐
          │  PostgreSQL  :5432   │
          └──────────────────────┘
```

**Key points:**

- All browser traffic goes through Caddy over HTTPS. No ports are exposed to the host.
- Caddy obtains TLS certificates automatically from Tailscale — no manual cert management.
- The backend validates JWTs using a static public certificate (`JWT_PUBLIC_KEY`). It never contacts Keycloak at runtime.
- Internal service-to-service traffic is plain HTTP over the isolated Docker network.

---

## Prerequisites

1. **Docker** and **Docker Compose** (v2+)
2. A **Tailscale** account with access to the admin console
3. A **Tailscale auth key** — generate one at [Tailscale Admin > Keys](https://login.tailscale.com/admin/settings/keys)
   - Check **Reusable** so the key survives container restarts
   - Check **Ephemeral** if you don't want the node to persist when the container stops

---

## Quick Start

```bash
# 1. Clone or download this folder
cd deploy

# 2. Run the interactive setup script
chmod +x setup.sh
./setup.sh
```

The script will:

1. Prompt for your Tailscale hostname and auth key
2. Generate secure database and Keycloak admin passwords
3. Build the Caddy image (with Tailscale plugin)
4. Pull the application images
5. If `JWT_PUBLIC_KEY` is not yet set, start only Keycloak + PostgreSQL for initial configuration
6. Once `JWT_PUBLIC_KEY` is provided, start the full stack

---

## Manual Setup (Step by Step)

If you prefer to set things up manually instead of using the script:

### Step 1: Create your `.env`

```bash
cp .env.example .env
```

Edit `.env` and fill in at minimum:

| Variable                  | What to set                                        |
| ------------------------- | -------------------------------------------------- |
| `TS_HOSTNAME`             | Machine name for Tailscale (e.g. `cart-and-cook`)  |
| `TS_AUTHKEY`              | Auth key from Tailscale admin console              |
| `DOMAIN`                  | Full FQDN (e.g. `cart-and-cook.your-tailnet.ts.net`) |
| `DB_PASSWORD`             | A strong random password                           |
| `KEYCLOAK_ADMIN_PASSWORD` | A strong random password                           |

Leave `JWT_PUBLIC_KEY` blank for now.

### Step 2: Build the Caddy image

```bash
docker compose build caddy
```

This builds Caddy with the Tailscale plugin. Only needed once (or after updating).

### Step 3: Pull application images

```bash
docker compose pull --ignore-buildable
```

### Step 4: Start Keycloak and PostgreSQL

```bash
docker compose up -d postgres keycloak caddy
```

Wait for Keycloak to become healthy:

```bash
docker compose logs -f keycloak
```

Look for `Listening on: http://0.0.0.0:8080` in the logs.

### Step 5: Configure Keycloak

Open Keycloak in your browser:

```
https://<DOMAIN>/auth/
```

Log in with the admin credentials from `.env`.

#### Create the realm

1. Click the realm dropdown (top-left) → **Create realm**
2. Realm name: `cart_and_cook`
3. Click **Create**

#### Create the OIDC client

1. Go to **Clients** → **Create client**
2. Fill in:
   - **Client ID**: `cart-and-cook-ui`
   - **Client type**: OpenID Connect
3. Click **Next**, then **Next** again (defaults are fine for public client)
4. Set:
   - **Valid redirect URIs**: `https://<DOMAIN>/*`
   - **Valid post logout redirect URIs**: `https://<DOMAIN>/*`
   - **Web origins**: `https://<DOMAIN>`
5. Click **Save**

#### Create a user

1. Go to **Users** → **Create user**
2. Fill in your name and email
3. Toggle **Email verified** to **On**
4. Click **Create**
5. Go to the **Credentials** tab → **Set password** → enter a password, toggle **Temporary** off

### Step 6: Get the JWT public certificate

1. Go to **Realm Settings** → **Keys**
2. Find the **RS256** row
3. Click **Certificate** (not "Public key")
4. Copy the entire base64 string

### Step 7: Set `JWT_PUBLIC_KEY` and start the full stack

Edit `.env` and paste the certificate:

```bash
JWT_PUBLIC_KEY=MIICqTCCAZECBgGc...the full base64 string...
```

Or re-run the setup script:

```bash
./setup.sh
```

Then start all services:

```bash
docker compose up -d
```

### Step 8: Access the app

Open your browser:

```
https://<DOMAIN>
```

You'll be redirected to Keycloak to log in, then back to the app.

---

## Environment Variables Reference

| Variable                  | Required | Default                             | Description                                        |
| ------------------------- | -------- | ----------------------------------- | -------------------------------------------------- |
| `TS_HOSTNAME`             | Yes      | —                                   | Tailscale machine name (used by Caddy)             |
| `TS_AUTHKEY`              | Yes      | —                                   | Tailscale auth key                                 |
| `DOMAIN`                  | Yes      | —                                   | Full FQDN (e.g. `cart-and-cook.your-tailnet.ts.net`) |
| `DB_USERNAME`             | No       | `postgres`                          | PostgreSQL username                                |
| `DB_PASSWORD`             | Yes      | —                                   | PostgreSQL password                                |
| `KEYCLOAK_ADMIN_USERNAME` | No       | `admin`                             | Keycloak admin username                            |
| `KEYCLOAK_ADMIN_PASSWORD` | Yes      | —                                   | Keycloak admin password                            |
| `JWT_PUBLIC_KEY`          | Yes      | —                                   | Base64 X.509 certificate from Keycloak             |
| `CART_AND_COOK_VERSION`   | No       | `release`                           | Docker image tag                                   |
| `AUTH_CLIENT_ID`          | No       | `cart-and-cook-ui`                  | Keycloak OIDC client ID                            |
| `OPENAI_API_KEY`          | No       | —                                   | OpenAI API key                                     |
| `HUGGINGFACE_API_KEY`     | No       | —                                   | Hugging Face API key                               |
| `OLLAMA_BASE_URL`         | No       | `http://host.docker.internal:11434` | Ollama API URL                                     |

---

## Common Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f backend

# Restart a service
docker compose restart backend

# Stop everything
docker compose down

# Stop and delete all data (database, volumes)
docker compose down -v

# Rebuild Caddy after Caddyfile changes
docker compose build caddy && docker compose up -d caddy

# Update to latest images
docker compose pull --ignore-buildable
docker compose up -d
```

---

## How JWT Validation Works

The backend validates JWT tokens **entirely offline** — it never contacts Keycloak at runtime.

1. At startup, the backend reads `JWT_PUBLIC_KEY` (a base64-encoded X.509 certificate)
2. It extracts the RSA public key from the certificate
3. For each incoming request, it:
   - Verifies the JWT signature against the public key
   - Checks the `iss` (issuer) claim matches `OAUTH2_ISSUER_URI`
   - Checks `exp` (expiration) and `nbf` (not-before) timestamps

**This means:**

- The backend starts instantly without waiting for Keycloak
- Network issues between backend and Keycloak don't affect API requests
- If Keycloak rotates its signing keys, you need to update `JWT_PUBLIC_KEY` and restart the backend

---

## How Keycloak Behind a Reverse Proxy Works

Keycloak is configured with:

- `KC_HTTP_RELATIVE_PATH=/auth` — serves under the `/auth` path
- `KC_PROXY_HEADERS=xforwarded` — trusts `X-Forwarded-*` headers from Caddy
- `KC_HOSTNAME=https://<DOMAIN>/auth` — generates correct URLs in tokens and discovery documents

This means:

- The `iss` (issuer) claim in JWTs will be `https://<DOMAIN>/auth/realms/cart_and_cook`
- The OIDC discovery endpoint is at `https://<DOMAIN>/auth/realms/cart_and_cook/.well-known/openid-configuration`
- Both the UI (`AUTH_AUTHORITY`) and backend (`OAUTH2_ISSUER_URI`) are auto-configured to match

---

## Troubleshooting

### Caddy won't start / Tailscale errors

- Verify `TS_AUTHKEY` is valid and not expired
- Check if the hostname is already taken: `docker compose logs caddy`
- Ensure `NET_ADMIN` capability is allowed (Docker Desktop: Settings → General → allow default capabilities)

### Keycloak takes a long time to start

Normal on first launch — it runs database migrations. Check progress:

```bash
docker compose logs -f keycloak
```

Wait for `Listening on: http://0.0.0.0:8080`.

### 401 Unauthorized from the API

1. **Check `JWT_PUBLIC_KEY`**: Must be the Certificate (not "Public key") from Keycloak's RS256 key
2. **Check issuer match**: The `iss` claim in the JWT must exactly match the backend's `OAUTH2_ISSUER_URI`. Both are auto-set to `https://<DOMAIN>/auth/realms/cart_and_cook` — verify with:
   ```bash
   # Decode a token (paste it into TOKEN)
   echo "$TOKEN" | cut -d '.' -f2 | base64 -D 2>/dev/null || echo "$TOKEN" | cut -d '.' -f2 | base64 -d
   ```
3. **Token expired**: Default Keycloak token lifetime is 5 minutes. Check `exp` in the decoded token.

### OIDC login redirects fail

- **Redirect URI mismatch**: In Keycloak client settings, `Valid redirect URIs` must include `https://<DOMAIN>/*`
- **Web origins**: Must include `https://<DOMAIN>`
- **Wrong authority**: The UI's `AUTH_AUTHORITY` must be `https://<DOMAIN>/auth/realms/cart_and_cook`

### Can't reach the app from another device

- Both devices must be on the same Tailscale network
- Run `tailscale status` on the client device to verify connectivity
- Try `curl https://<DOMAIN>/api/actuator/health`

### Keycloak key rotation

If you rotate Keycloak's signing keys:

1. Go to Keycloak → Realm Settings → Keys → RS256 → Certificate
2. Copy the new certificate
3. Update `JWT_PUBLIC_KEY` in `.env`
4. Restart the backend: `docker compose restart backend`

---

## Updating

```bash
cd deploy

# Pull latest images
docker compose pull --ignore-buildable

# Restart with new images
docker compose up -d
```

To pin a specific version, set `CART_AND_COOK_VERSION` in `.env`:

```bash
CART_AND_COOK_VERSION=1.0.0
```
