# Cooksyne — Deployment Guide

Self-hosted deployment of Cooksyne using Docker Compose.

By default the stack includes Keycloak (authentication), Caddy with Tailscale (HTTPS reverse proxy), and Ollama (local AI). Each of these is optional — you can bring your own auth server, reverse proxy, or AI provider.

## Architecture

```
Browser
  │
  │  HTTPS
  ▼
┌─────────────────────────────────────────┐
│  Reverse Proxy (Caddy or your own)      │
│                                         │
│  /           → UI        (nginx:80)     │
│  /auth/*     → Keycloak  (8080)         │
│  /api/*      → Backend   (8081)         │
└─────────┬───────────┬───────────────────┘
          │           │
    ┌─────┘     ┌─────┘
    ▼           ▼
┌────────┐ ┌────────┐ ┌────────────┐ ┌────────────┐
│  UI    │ │Backend │ │ Keycloak   │ │  Ollama    │
│ nginx  │ │ :8081  │ │ (optional) │ │ (optional) │
└────────┘ └───┬────┘ └─────┬──────┘ └────────────┘
               │             │
               ▼             ▼
          ┌──────────────────────┐
          │  PostgreSQL  :5432   │
          └──────────────────────┘
```

**Key points:**

- All browser traffic goes through the reverse proxy over HTTPS. No ports are exposed to the host by default.
- The backend validates JWTs using a static public certificate (`JWT_PUBLIC_KEY`). It never contacts the auth server at runtime.
- Internal service-to-service traffic is plain HTTP over the isolated Docker network.
- Optional services (Keycloak, Caddy, Ollama) are controlled via Docker Compose **profiles**.

---

## Prerequisites

1. **Docker** and **Docker Compose** (v2+)
2. An **OIDC-compatible auth server** — bundled Keycloak, or your own (Auth0, Okta, Azure AD, etc.)
3. _(If using Caddy)_ A **Tailscale** account and auth key from [Tailscale Admin > Keys](https://login.tailscale.com/admin/settings/keys)

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

1. Ask which optional components to include (Keycloak, Caddy, Ollama)
2. Prompt for your domain, credentials, and AI provider
3. Generate secure database and Keycloak passwords
4. Build images and pull containers
5. If `JWT_PUBLIC_KEY` is not yet set, start Keycloak + PostgreSQL for initial configuration
6. Once `JWT_PUBLIC_KEY` is provided, start the full stack

---

## Docker Compose Profiles

Optional services use [Docker Compose profiles](https://docs.docker.com/compose/profiles/). Set `COMPOSE_PROFILES` in `.env` to control which services start:

| Profile    | Service  | When to include                          |
| ---------- | -------- | ---------------------------------------- |
| `keycloak` | Keycloak | Using bundled Keycloak for auth          |
| `caddy`    | Caddy    | Using Caddy + Tailscale as reverse proxy |
| `ollama`   | Ollama   | Using local Ollama for AI                |

**Examples:**

```bash
# Full stack (default from setup.sh)
COMPOSE_PROFILES=keycloak,caddy,ollama

# No Caddy — bring your own reverse proxy
COMPOSE_PROFILES=keycloak,ollama

# No Keycloak — bring your own auth (Auth0, Okta, etc.)
COMPOSE_PROFILES=caddy,ollama

# Minimal — own proxy + own auth, no local AI
COMPOSE_PROFILES=
```

Services **without** a profile (PostgreSQL, Backend, UI) always start.

---

## Manual Setup (Step by Step)

If you prefer to set things up manually instead of using the script:

### Step 1: Create your `.env`

```bash
cp .env.example .env
```

Edit `.env` and fill in at minimum:

| Variable               | What to set                                                     |
| ---------------------- | --------------------------------------------------------------- |
| `COMPOSE_PROFILES`     | Which optional services to start (e.g. `keycloak,caddy,ollama`) |
| `DOMAIN`               | Full FQDN (e.g. `cooksyne.your-tailnet.ts.net`)            |
| `API_URL`              | `https://<DOMAIN>/api`                                          |
| `CORS_ALLOWED_ORIGINS` | `https://<DOMAIN>`                                              |
| `OAUTH2_ISSUER_URI`    | `https://<DOMAIN>/auth/realms/cooksyne` (for Keycloak)     |
| `AUTH_AUTHORITY`       | Same as `OAUTH2_ISSUER_URI`                                     |
| `DB_PASSWORD`          | A strong random password                                        |

If using Keycloak, also set `KEYCLOAK_ADMIN_PASSWORD`.
If using Caddy, also set `TS_HOSTNAME` and `TS_AUTHKEY`.

Leave `JWT_PUBLIC_KEY` blank for now.

### Step 2: Build the Caddy image (if using Caddy)

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
docker compose up -d postgres keycloak
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
2. Realm name: `cooksyne`
3. Click **Create**

#### Create the OIDC client

1. Go to **Clients** → **Create client**
2. Fill in:
   - **Client ID**: `cooksyne-ui`
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

You'll be redirected to your auth provider to log in, then back to the app.

---

## AI Configuration

Cooksyne uses AI to import recipes from images and URLs. AI is **optional** — the app works without it.

### Choosing a provider

Set `AI_PROVIDER` in `.env`:

| Value         | Description                         | Requires              |
| ------------- | ----------------------------------- | --------------------- |
| `ollama`      | Local AI via Ollama (default, free) | `ollama` profile      |
| `openai`      | OpenAI API                          | `OPENAI_API_KEY`      |
| `huggingface` | Hugging Face Inference API          | `HUGGINGFACE_API_KEY` |
| `bedrock`     | AWS Bedrock                         | AWS credentials       |
| `disabled`    | No AI features                      | Nothing               |

### Runtime configuration

The active AI provider and model can be changed at any time through the app's **Settings > Runtime** page — no restart required. The `.env` setting controls which provider beans are loaded at startup; the runtime UI overrides which one is actually used.

### Ollama (recommended for self-hosting)

Ollama runs locally with no API keys or cloud dependencies. The bundled Ollama container starts when the `ollama` profile is active.

**After starting the stack, pull a model:**

```bash
docker compose exec ollama ollama pull qwen2.5:1.5b
```

**Recommended models for CPU-only hosts (4 cores, 8-16 GB RAM):**

| Model          | Size | Speed   | Quality | Notes                        |
| -------------- | ---- | ------- | ------- | ---------------------------- |
| `qwen2.5:1.5b` | 1.5B | Fast    | Good    | Recommended — best balance   |
| `phi3:mini`    | 3.8B | Medium  | Better  | More capable, needs more RAM |
| `tinyllama`    | 1.1B | Fastest | Lower   | Lightest option              |

Select the model in **Settings > Runtime** in the app.

**Ollama tuning (`.env`):**

| Variable             | Default               | Description             |
| -------------------- | --------------------- | ----------------------- |
| `OLLAMA_BASE_URL`    | `http://ollama:11434` | Ollama API endpoint     |
| `OLLAMA_NUM_CTX`     | `2048`                | Context window (tokens) |
| `OLLAMA_NUM_THREAD`  | `4`                   | CPU threads (0 = auto)  |
| `OLLAMA_NUM_PREDICT` | `512`                 | Max output tokens       |

To use an **external Ollama instance** instead of the bundled container, remove `ollama` from `COMPOSE_PROFILES` and set `OLLAMA_BASE_URL` (e.g. `http://host.docker.internal:11434`).

### OpenAI

```bash
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
```

Default model: `gpt-4o-mini` (changeable in Settings > Runtime).

### AWS Bedrock

```bash
AI_PROVIDER=bedrock
```

Uses the default AWS credential chain (env vars, instance role, `~/.aws/credentials`). Default model: `anthropic.claude-3-haiku-20240307-v1:0`, default region: `us-east-1`. Both are changeable in Settings > Runtime.

### Hugging Face

```bash
AI_PROVIDER=huggingface
HUGGINGFACE_API_KEY=hf_...
```

Default model: `Salesforce/blip-image-captioning-large` (changeable in Settings > Runtime).

---

## Using Your Own Reverse Proxy

If you don't want to use the bundled Caddy + Tailscale setup, remove `caddy` from `COMPOSE_PROFILES` and configure your own reverse proxy.

### Required routes

Your proxy must forward these paths to the Docker containers:

| Path      | Upstream                              | Notes                                      |
| --------- | ------------------------------------- | ------------------------------------------ |
| `/`       | `ui:80` (or `localhost:3000`)         | Frontend — catch-all                       |
| `/api/*`  | `backend:8081` (or `localhost:8081`)  | Backend API — pass path as-is              |
| `/auth/*` | `keycloak:8080` (or `localhost:8080`) | **Strip `/auth` prefix** before forwarding |

**Important:** The `/auth` prefix must be stripped before forwarding to Keycloak. Keycloak serves at `/` internally.

### Exposing ports

Uncomment the `ports:` sections in `docker-compose.yml` for the services your proxy needs to reach:

```yaml
# In docker-compose.yml:
backend:
  ports:
    - "8081:8081"

ui:
  ports:
    - "3000:80"
```

### Example: Nginx

```nginx
server {
    listen 443 ssl;
    server_name cook.example.com;

    ssl_certificate     /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Keycloak — strip /auth prefix
    location /auth/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Example: Traefik (Docker labels)

```yaml
# Add to docker-compose.yml services:
backend:
  labels:
    - "traefik.http.routers.api.rule=Host(`cook.example.com`) && PathPrefix(`/api`)"
    - "traefik.http.services.api.loadbalancer.server.port=8081"

ui:
  labels:
    - "traefik.http.routers.ui.rule=Host(`cook.example.com`)"
    - "traefik.http.routers.ui.priority=1"
    - "traefik.http.services.ui.loadbalancer.server.port=80"

keycloak:
  labels:
    - "traefik.http.routers.auth.rule=Host(`cook.example.com`) && PathPrefix(`/auth`)"
    - "traefik.http.middlewares.strip-auth.stripprefix.prefixes=/auth"
    - "traefik.http.routers.auth.middlewares=strip-auth"
    - "traefik.http.services.auth.loadbalancer.server.port=8080"
```

---

## Using Your Own Auth Server

Cooksyne works with **any OIDC-compatible identity provider**. If you don't want to use Keycloak, remove `keycloak` from `COMPOSE_PROFILES` and configure your provider.

### What the backend needs

| Variable            | Description                                                                                    |
| ------------------- | ---------------------------------------------------------------------------------------------- |
| `JWT_PUBLIC_KEY`    | Base64-encoded X.509 certificate (RS256 signing key). No PEM headers — just the base64 string. |
| `OAUTH2_ISSUER_URI` | The JWT `iss` claim value (e.g. `https://tenant.auth0.com/`)                                   |

The backend validates tokens **offline** using the public key. It never calls the auth server.

### What the UI needs

| Variable         | Description                                                                            |
| ---------------- | -------------------------------------------------------------------------------------- |
| `AUTH_AUTHORITY` | OIDC discovery base URL — the UI fetches `/.well-known/openid-configuration` from here |
| `AUTH_CLIENT_ID` | The OIDC client ID registered with your provider                                       |

### Provider-specific notes

**Auth0:**

```bash
OAUTH2_ISSUER_URI=https://YOUR_TENANT.auth0.com/
AUTH_AUTHORITY=https://YOUR_TENANT.auth0.com/
AUTH_CLIENT_ID=your-client-id
# JWT_PUBLIC_KEY: Download from https://YOUR_TENANT.auth0.com/.well-known/jwks.json
# Extract the RS256 x5c certificate value (first entry in the array).
```

**Okta:**

```bash
OAUTH2_ISSUER_URI=https://YOUR_ORG.okta.com/oauth2/default
AUTH_AUTHORITY=https://YOUR_ORG.okta.com/oauth2/default
AUTH_CLIENT_ID=your-client-id
# JWT_PUBLIC_KEY: Download from https://YOUR_ORG.okta.com/oauth2/default/v1/keys
# Extract the RS256 x5c certificate value.
```

**Azure AD:**

```bash
OAUTH2_ISSUER_URI=https://login.microsoftonline.com/YOUR_TENANT_ID/v2.0
AUTH_AUTHORITY=https://login.microsoftonline.com/YOUR_TENANT_ID/v2.0
AUTH_CLIENT_ID=your-client-id
# JWT_PUBLIC_KEY: Download from https://login.microsoftonline.com/YOUR_TENANT_ID/discovery/v2.0/keys
# Extract the RS256 x5c certificate value.
```

### Getting the JWT public key from any OIDC provider

1. Find your provider's JWKS endpoint (usually at `<issuer>/.well-known/openid-configuration` → `jwks_uri`)
2. Fetch the JWKS JSON
3. Find the key with `"use": "sig"` and `"alg": "RS256"`
4. Copy the first value from the `x5c` array — this is the base64 X.509 certificate
5. Paste it as `JWT_PUBLIC_KEY` in `.env`

---

## Environment Variables Reference

### Core (always required)

| Variable                | Required | Default            | Description                                          |
| ----------------------- | -------- | ------------------ | ---------------------------------------------------- |
| `COMPOSE_PROFILES`      | No       | _(empty)_          | Comma-separated profiles: `keycloak,caddy,ollama`    |
| `DOMAIN`                | Yes      | —                  | Full FQDN (e.g. `cooksyne.your-tailnet.ts.net`) |
| `API_URL`               | Yes      | —                  | Browser-facing API URL (e.g. `https://<DOMAIN>/api`) |
| `CORS_ALLOWED_ORIGINS`  | Yes      | —                  | Comma-separated allowed origins                      |
| `OAUTH2_ISSUER_URI`     | Yes      | —                  | JWT issuer — must match `iss` claim in tokens        |
| `AUTH_AUTHORITY`        | Yes      | —                  | OIDC discovery base URL for the UI                   |
| `DB_USERNAME`           | No       | `postgres`         | PostgreSQL username                                  |
| `DB_PASSWORD`           | Yes      | —                  | PostgreSQL password                                  |
| `JWT_PUBLIC_KEY`        | Yes      | —                  | Base64 X.509 signing certificate                     |
| `COOKSYNE_VERSION` | No       | `release`          | Docker image tag                                     |
| `AUTH_CLIENT_ID`        | No       | `cooksyne-ui` | OIDC client ID                                       |

### Keycloak (when `keycloak` profile is active)

| Variable                  | Required | Default | Description             |
| ------------------------- | -------- | ------- | ----------------------- |
| `KEYCLOAK_ADMIN_USERNAME` | No       | `admin` | Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | Yes      | —       | Keycloak admin password |

### Caddy / Tailscale (when `caddy` profile is active)

| Variable      | Required | Default | Description            |
| ------------- | -------- | ------- | ---------------------- |
| `TS_HOSTNAME` | Yes      | —       | Tailscale machine name |
| `TS_AUTHKEY`  | Yes      | —       | Tailscale auth key     |

### AI

| Variable              | Required | Default               | Description                                              |
| --------------------- | -------- | --------------------- | -------------------------------------------------------- |
| `AI_PROVIDER`         | No       | `ollama`              | `ollama`, `openai`, `huggingface`, `bedrock`, `disabled` |
| `OPENAI_API_KEY`      | No       | —                     | OpenAI API key                                           |
| `HUGGINGFACE_API_KEY` | No       | —                     | Hugging Face API key                                     |
| `OLLAMA_BASE_URL`     | No       | `http://ollama:11434` | Ollama API URL                                           |
| `OLLAMA_NUM_CTX`      | No       | `2048`                | Context window size (tokens)                             |
| `OLLAMA_NUM_THREAD`   | No       | `4`                   | CPU threads (0 = auto)                                   |
| `OLLAMA_NUM_PREDICT`  | No       | `512`                 | Max output tokens                                        |

---

## Common Commands

```bash
# Start all services (uses COMPOSE_PROFILES from .env)
docker compose up -d

# Start specific profiles only
docker compose --profile keycloak --profile ollama up -d

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

# Pull an Ollama model
docker compose exec ollama ollama pull qwen2.5:1.5b
```

---

## How JWT Validation Works

The backend validates JWT tokens **entirely offline** — it never contacts the auth server at runtime.

1. At startup, the backend reads `JWT_PUBLIC_KEY` (a base64-encoded X.509 certificate)
2. It extracts the RSA public key from the certificate
3. For each incoming request, it:
   - Verifies the JWT signature against the public key
   - Checks the `iss` (issuer) claim matches `OAUTH2_ISSUER_URI`
   - Checks `exp` (expiration) and `nbf` (not-before) timestamps

**This means:**

- The backend starts instantly without waiting for the auth server
- Network issues between backend and auth server don't affect API requests
- If the auth server rotates its signing keys, you need to update `JWT_PUBLIC_KEY` and restart the backend

---

## How Keycloak Behind a Reverse Proxy Works

Keycloak is configured with:

- `KC_PROXY_HEADERS=xforwarded` — trusts `X-Forwarded-*` headers from the reverse proxy
- `KC_HOSTNAME=https://<DOMAIN>/auth` — generates correct URLs in tokens and discovery documents

The reverse proxy (Caddy by default) strips the `/auth` prefix before forwarding to Keycloak, which runs at `/` internally.

This means:

- The `iss` (issuer) claim in JWTs will be `https://<DOMAIN>/auth/realms/cooksyne`
- The OIDC discovery endpoint is at `https://<DOMAIN>/auth/realms/cooksyne/.well-known/openid-configuration`
- Both the UI (`AUTH_AUTHORITY`) and backend (`OAUTH2_ISSUER_URI`) must be set to `https://<DOMAIN>/auth/realms/cooksyne`

---

## Troubleshooting

### Caddy won't start / Tailscale errors

- Verify `TS_AUTHKEY` is valid and not expired
- Check if the hostname is already taken: `docker compose logs caddy`
- Ensure `NET_ADMIN` capability is allowed (Docker Desktop: Settings > General > allow default capabilities)

### Keycloak takes a long time to start

Normal on first launch — it runs database migrations. Check progress:

```bash
docker compose logs -f keycloak
```

Wait for `Listening on: http://0.0.0.0:8080`.

### 401 Unauthorized from the API

1. **Check `JWT_PUBLIC_KEY`**: Must be the Certificate (not "Public key") from your auth provider's RS256 key
2. **Check issuer match**: The `iss` claim in the JWT must exactly match `OAUTH2_ISSUER_URI`. Verify with:
   ```bash
   # Decode a token (paste it into TOKEN)
   echo "$TOKEN" | cut -d '.' -f2 | base64 -D 2>/dev/null || echo "$TOKEN" | cut -d '.' -f2 | base64 -d
   ```
3. **Token expired**: Default Keycloak token lifetime is 5 minutes. Check `exp` in the decoded token.

### OIDC login redirects fail

- **Redirect URI mismatch**: In your auth provider's client settings, redirect URIs must include `https://<DOMAIN>/*`
- **Web origins**: Must include `https://<DOMAIN>`
- **Wrong authority**: The UI's `AUTH_AUTHORITY` must match the OIDC discovery URL

### Can't reach the app from another device

- Both devices must be on the same Tailscale network (if using Caddy + Tailscale)
- Run `tailscale status` on the client device to verify connectivity
- Try `curl https://<DOMAIN>/api/actuator/health`

### Keycloak key rotation

If you rotate Keycloak's signing keys:

1. Go to Keycloak > Realm Settings > Keys > RS256 > Certificate
2. Copy the new certificate
3. Update `JWT_PUBLIC_KEY` in `.env`
4. Restart the backend: `docker compose restart backend`

### AI not working

1. **Check provider**: Verify `AI_PROVIDER` is set correctly in `.env`
2. **Ollama model not pulled**: Run `docker compose exec ollama ollama pull qwen2.5:1.5b`
3. **API key missing**: For OpenAI/HuggingFace, check the API key in `.env`
4. **Check runtime settings**: The Settings > Runtime page in the app overrides `.env` — verify the correct provider and model are selected

---

## Updating

```bash
cd deploy

# Pull latest images
docker compose pull --ignore-buildable

# Restart with new images
docker compose up -d
```

To pin a specific version, set `COOKSYNE_VERSION` in `.env`:

```bash
COOKSYNE_VERSION=1.0.0
```
