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

## Production Readiness Notes

Before production deployment:

1. Provide API keys with environment variables for any AI provider that requires them.
2. Use managed secret delivery for DB/API credentials.
3. Disable overly verbose SQL logging if not needed.
4. Place process under a supervisor if relying on auto-restart behavior.
5. Restrict CORS origins to trusted domains only.
