# Cart and Cook Backend

Comprehensive guide for setting up, running, and operating the Cart and Cook backend.

This repository contains the Spring Boot multi-module backend used by the UI application. It supports authenticated recipe and grocery list management, runtime settings management, image upload/serving, and runtime-configurable AI providers.

## Repository Structure

Main modules (from Gradle settings):

- `core`
- `runtime:self-hosted`
- `adapters:persistence-jpa`
- `adapters:auth-local`
- `adapters:ai-ollama`
- `adapters:ai-openai`
- `adapters:ai-bedrock`
- `adapters:ai-huggingface`

Primary runnable service for local development:

- `runtime:self-hosted`

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Security OAuth2 Resource Server (JWT)
- Spring Data JPA
- PostgreSQL
- Gradle Kotlin DSL
- Optional AI providers: Ollama, OpenAI, AWS Bedrock, Hugging Face

## Prerequisites

Install/configure:

1. JDK 21
2. PostgreSQL (local or remote)
3. Keycloak (or compatible OIDC provider) for JWT issuer
4. Optional AI services based on provider selection:
   - Ollama local runtime
   - OpenAI account/key
   - AWS account/credentials for Bedrock
   - Hugging Face API key

## Recommended Startup Sequence

Use this order for the smoothest local startup.

1. Start PostgreSQL and confirm the database is reachable.
2. Start Keycloak and confirm realm issuer is available at `http://localhost:8080/realms/cart_and_cook` (or your configured issuer).
3. Export backend core runtime environment variables in the same terminal you will use to launch the app:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/cart_and_cook'
export DB_USERNAME='postgres'
export DB_PASSWORD=''
export OAUTH2_ISSUER_URI='http://localhost:8080/realms/cart_and_cook'
export PORT='8081'
export OPENAI_API_KEY='your-openai-key'
export HUGGINGFACE_API_KEY='your-huggingface-key'
```

4. Start backend runtime:

```bash
./gradlew :runtime:self-hosted:bootRun
```

5. Verify backend health by opening any protected endpoint with a valid token flow from the UI login path.
6. Start the UI repository and then authenticate.
7. Use `/settings/runtime` to configure AI provider keys/models if needed.

## Quick Start

1. Clone repository.
2. Ensure PostgreSQL is reachable.
3. Ensure Keycloak issuer URL is available.
4. Run the self-hosted runtime:

```bash
./gradlew :runtime:self-hosted:bootRun
```

Default API base URL:

- `http://localhost:8081`

## Default Boot-Time Configuration

The following defaults apply from `runtime/self-hosted/src/main/resources/application.yml` unless overridden by environment variables.

### Database

- `DB_URL` default: `jdbc:postgresql://localhost:5432/cart_and_cook`
- `DB_USERNAME` default: `postgres`
- `DB_PASSWORD` default: empty string
- JPA `ddl-auto`: `update`
- SQL logging: enabled

### Security

- `OAUTH2_ISSUER_URI` default: `http://localhost:8080/realms/cart_and_cook`
- Resource server validates JWT issuer URI

### Server

- `PORT` default: `8081`

### AI API Keys (Environment Variables)

- `OPENAI_API_KEY` default: empty
- `HUGGINGFACE_API_KEY` default: empty

Important:

- These keys are read from environment variables at startup and are not stored in runtime settings.

## Runtime Configuration API

The service exposes runtime config endpoints at `/config/runtime`.

Endpoints:

1. `GET /config/runtime`
2. `PUT /config/runtime`

These are authenticated endpoints.

## Runtime Config Defaults and Behavior

Runtime config includes:

- AI provider and provider-specific settings

Core runtime settings are not persisted from UI and are not editable via runtime config API.
Core settings are startup-only and must be provided as environment variables when launching the backend process.

Default runtime values when unset:

- `aiProvider`: empty (no provider selected)
- `ollamaBaseUrl`: `http://localhost:11434`
- `ollamaModel`: `llava-phi3`
- `openAiModel`: `gpt-4o-mini`
- `awsRegion`: `us-east-1`
- `bedrockModelId`: `anthropic.claude-3-haiku-20240307-v1:0`
- `huggingFaceModel`: `Salesforce/blip-image-captioning-large`

## Core Runtime Settings (Startup Environment Variables)

Set these in your shell/process manager before launching the backend.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OAUTH2_ISSUER_URI`
- `PORT`
- `OPENAI_API_KEY` (required for OpenAI provider)
- `HUGGINGFACE_API_KEY` (required for Hugging Face provider)

Example:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/cart_and_cook'
export DB_USERNAME='postgres'
export DB_PASSWORD=''
export OAUTH2_ISSUER_URI='http://localhost:8080/realms/cart_and_cook'
export PORT='8081'
export OPENAI_API_KEY='your-openai-key'
export HUGGINGFACE_API_KEY='your-huggingface-key'
./gradlew :runtime:self-hosted:bootRun
```

## Authentication and Authorization Defaults

Security defaults:

- All endpoints require authentication except:
  - `/public/**`
  - `GET /api/images/**`
- OAuth2 JWT resource server is enabled

User behavior:

- User is resolved from JWT `issuer + subject`
- If first login and email is verified, backend auto-provisions user and links external identity

CORS defaults:

- Allowed origins:
  - `http://localhost:4200`
  - `http://localhost:9090`
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `Authorization, Content-Type`
- Credentials: allowed

## Core API Surface

### Recipes

- `GET /recipes`
- `GET /recipes/{id}`
- `POST /recipes` (upsert)
- `DELETE /recipes/{id}`

Recipe ownership is user-scoped via authenticated user context.

### Grocery Lists

- `GET /grocery_list`
- `GET /grocery_list/{id}`
- `POST /grocery_list` (upsert)
- `DELETE /grocery_list/{id}`

Grocery lists are also user-scoped.

### Images

- `POST /api/images` (multipart upload, authenticated)
- `GET /api/images/{userId}/{filename}` (public read)

Default storage path:

- `${user.home}/.cartandcook/images`

Storage behavior:

- images are saved in per-user subfolders
- filename is generated UUID + extension
- allowed extensions: `jpg, jpeg, png, gif, webp`
- deleting a recipe attempts to delete the associated image file

### AI Analysis

- `POST /api/ai/analyze-food` (multipart image)
- `POST /api/ai/analyze-recipe` (multipart image)

Provider is selected dynamically from persisted runtime config.

## Environment Variable Reference

Set these when launching runtime service:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OAUTH2_ISSUER_URI`
- `PORT`
- `OPENAI_API_KEY` (required for OpenAI provider)
- `HUGGINGFACE_API_KEY` (required for Hugging Face provider)
- `CARTANDCOOK_STORAGE_IMAGE_DIR` (optional override for image storage directory via Spring property binding style if configured externally)

## Typical Local Development Workflow

1. Start PostgreSQL.
2. Start Keycloak with realm `cart_and_cook` issuer matching configured value.
3. Start backend:

```bash
./gradlew :runtime:self-hosted:bootRun
```

4. Start UI from companion repository on `http://localhost:4200`.
5. Authenticate through OIDC.
6. Use `/settings/runtime` in UI for AI runtime updates only.

## Build and Test Commands

From repository root:

- Compile all: `./gradlew compileJava`
- Run tests: `./gradlew test`
- Run self-hosted runtime: `./gradlew :runtime:self-hosted:bootRun`

## Troubleshooting

### Startup Fails with DB Connection Errors

- Verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Confirm PostgreSQL is running and reachable.
- Confirm these values are exported in the terminal (or process manager) used to start the backend.

### 401 Unauthorized from API

- Verify JWT issuer equals configured `OAUTH2_ISSUER_URI`.
- Confirm client obtains token from expected realm.
- Confirm request includes bearer token.

### Core Runtime Setting Changes Not Applying from UI

- This is expected.
- Core runtime settings are startup environment variables and are not editable from `/settings/runtime`.
- Update env vars and restart the backend process.

### Images Not Loading

- Ensure uploaded image URL format is `/api/images/{userId}/{filename}`.
- Confirm file exists under image storage directory.
- Confirm frontend is using backend base URL that serves images.

## Docker Deployment (Self-Hosted)

Users don't need to clone the source repos. Just download two files and run.

### Quick Start

```bash
mkdir cart-and-cook && cd cart-and-cook
curl -LO https://raw.githubusercontent.com/Jordan890/cart_and_cook/main/deploy/docker-compose.yml
curl -LO https://raw.githubusercontent.com/Jordan890/cart_and_cook/main/deploy/.env.example
cp .env.example .env    # edit .env with your values
docker compose up -d
```

Or use the setup script (generates secrets automatically):

```bash
curl -LO https://raw.githubusercontent.com/Jordan890/cart_and_cook/main/deploy/setup.sh
chmod +x setup.sh
./setup.sh
```

### Updating

```bash
docker compose pull
docker compose up -d
```

To pin a specific version instead of latest, set `CART_AND_COOK_VERSION` in `.env`:

```bash
CART_AND_COOK_VERSION=1.0.0
```

### Services

| Service    | Internal Port | Default External Port | URL                     |
| ---------- | ------------- | --------------------- | ----------------------- |
| Frontend   | 80            | 3000                  | `http://localhost:3000` |
| Backend    | 8081          | 8081                  | `http://localhost:8081` |
| Keycloak   | 8080          | 8080                  | `http://localhost:8080` |
| PostgreSQL | 5432          | 5432                  | `localhost:5432`        |

### Configuration

All settings are in `deploy/.env`. Copy from the example and edit:

```bash
cp .env.example .env
```

Key variables:

| Variable                  | Purpose                                                  | Default                                      |
| ------------------------- | -------------------------------------------------------- | -------------------------------------------- |
| `DB_PASSWORD`             | PostgreSQL password                                      | (generated by setup.sh)                      |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password                                  | (generated by setup.sh)                      |
| `AI_PROVIDER`             | AI service: `ollama`, `openai`, `bedrock`, `huggingface` | `ollama`                                     |
| `API_URL`                 | Backend URL as seen by the browser                       | `http://localhost:8081`                      |
| `AUTH_AUTHORITY`          | Keycloak URL as seen by the browser                      | `http://localhost:8080/realms/cart_and_cook` |
| `FRONTEND_PORT`           | Host port for the frontend                               | `3000`                                       |
| `BACKEND_PORT`            | Host port for the backend                                | `8081`                                       |

See `deploy/.env.example` for the full list including AI provider-specific variables.

### Using an External PostgreSQL or Keycloak

If you already have PostgreSQL and/or Keycloak running, you don't need Docker to manage them. Set these in `.env`:

**External database only:**

```bash
USE_EXTERNAL_DB=true
DB_URL=jdbc:postgresql://your-db-host:5432/cart_and_cook
DB_USERNAME=postgres
DB_PASSWORD=your-password
```

**External Keycloak only:**

```bash
USE_EXTERNAL_KEYCLOAK=true
OAUTH2_ISSUER_URI=http://your-keycloak-host:8080/realms/cart_and_cook
AUTH_AUTHORITY=http://your-keycloak-host:8080/realms/cart_and_cook
```

**Both external:**

```bash
USE_EXTERNAL_DB=true
USE_EXTERNAL_KEYCLOAK=true
DB_URL=jdbc:postgresql://your-db-host:5432/cart_and_cook
OAUTH2_ISSUER_URI=http://your-keycloak-host:8080/realms/cart_and_cook
AUTH_AUTHORITY=http://your-keycloak-host:8080/realms/cart_and_cook
```

Then run `./setup.sh` as normal — it will only start the services you need (backend + frontend, plus whichever infrastructure services are not external).

### Keycloak Setup After First Launch

After containers start, configure Keycloak:

1. Open `http://localhost:8080` and log in with the admin credentials from `.env`
2. Create a new realm named `cart_and_cook`
3. In the realm, create a client:
   - Client ID: `cart-and-cook-ui`
   - Client type: OpenID Connect
   - Valid redirect URIs: `http://localhost:3000/*`
   - Web origins: `http://localhost:3000`
4. Create a user account and set a password
5. Open `http://localhost:3000` and log in

### Building Images Locally (Contributors)

If you're developing and want to build from source instead of pulling published images:

```bash
# Backend (from cart_and_cook root)
docker build -t ghcr.io/jordan890/cart-and-cook-api:local .

# Frontend (from cart_and_cook_ui/cart-and-cook-ui)
docker build -t ghcr.io/jordan890/cart-and-cook-ui:local .
```

Then set in `.env`:

```bash
CART_AND_COOK_VERSION=local
```

### Health Checks

The backend exposes a health endpoint via Spring Boot Actuator:

```
GET /actuator/health
```

This endpoint is unauthenticated and used by Docker health checks. The frontend health check verifies nginx is serving on port 80 inside the container.

### Docker Commands

```bash
cd deploy

# Start all services
docker compose up -d

# Follow logs
docker compose logs -f

# Follow logs for one service
docker compose logs -f backend

# Restart a service
docker compose restart backend

# Stop all services
docker compose down

# Stop and delete all data (database, volumes)
docker compose down -v
```

### Reverse Proxy (Optional)

A sample nginx reverse proxy config is provided at `deploy/nginx-proxy.conf`. This routes all traffic through a single port:

- `/` → Frontend
- `/api/` → Backend
- `/auth/` → Keycloak

To use it, add a `proxy` service to `docker-compose.yml` and remove the port mappings from the individual services. The config file includes commented-out TLS configuration.

### Versioning

Images are tagged with version numbers and the `release` tag (latest stable). Control which version to use via `.env`:

```bash
# Latest stable (default)
CART_AND_COOK_VERSION=release

# Specific version
CART_AND_COOK_VERSION=1.2.0
```

Then pull the new version:

```bash
docker compose pull
docker compose up -d
```

### Accessing Ollama from Docker

By default, the Ollama base URL is set to `http://host.docker.internal:11434`, which connects to Ollama running on your host machine. If running Ollama in a separate container, update `OLLAMA_BASE_URL` in `.env` to point to that container's address.

## Production Readiness Notes

Before production deployment:

1. Provide API keys with environment variables for any AI provider that requires them.
2. Use managed secret delivery for DB/API credentials.
3. Disable overly verbose SQL logging if not needed.
4. Enable TLS via the reverse proxy config or a load balancer.
5. Restrict CORS origins to trusted domains only — set `CORS_ALLOWED_ORIGINS` in `.env`.
6. Consider switching `spring.jpa.hibernate.ddl-auto` from `update` to `validate` and using a migration tool (Flyway/Liquibase) for schema management.

## Windows Users

Docker Compose and image pulls work the same on Windows (via Docker Desktop or WSL2). The `setup.sh` script requires a Bash shell, so either:

- **Run from WSL2** (recommended): Open a WSL terminal and run `bash setup.sh` as normal.
- **Skip the script** and do the three steps manually in PowerShell:

```powershell
Copy-Item .env.example .env
# Edit .env with your settings
docker compose pull
docker compose up -d
```
