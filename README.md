# Cooksyne

A self-hosted recipe and grocery list manager with AI-powered food analysis. Upload a photo of food or a recipe, and Cooksyne identifies it and can generate a grocery list — all running on your own infrastructure.

## Self-Hosting (Docker)

The recommended way to run Cooksyne is with Docker Compose. The `deploy/` directory contains the full stack including the frontend, backend, PostgreSQL, and Keycloak.

**Quick start:**

```bash
git clone https://github.com/Jordan890/cooksyne.git
cd cooksyne/deploy
chmod +x setup.sh
./setup.sh
```

See the **[deployment guide](deploy/README.md)** for full instructions including AI setup, auth configuration, reverse proxy options, and more.

---

## Local Development Setup

If you want to contribute or run from source, follow this section instead of Docker.

### Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Security OAuth2 Resource Server (JWT)
- Spring Data JPA
- PostgreSQL
- Gradle Kotlin DSL
- Optional AI providers: Ollama, OpenAI, AWS Bedrock, Hugging Face

### Prerequisites

1. JDK 21
2. PostgreSQL (local or remote)
3. Keycloak (or compatible OIDC provider) for JWT issuer
4. Optional AI services based on provider selection:
   - Ollama local runtime
   - OpenAI account/key
   - AWS account/credentials for Bedrock
   - Hugging Face API key

### Startup Sequence

1. Start PostgreSQL and confirm the database is reachable.
2. Start Keycloak and confirm the realm issuer is available at `http://localhost:8080/realms/cooksyne`.
3. Export environment variables:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/cooksyne'
export DB_USERNAME='postgres'
export DB_PASSWORD=''
export OAUTH2_ISSUER_URI='http://localhost:8080/realms/cooksyne'
export PORT='8081'
export OPENAI_API_KEY='your-openai-key'         # if using OpenAI
export HUGGINGFACE_API_KEY='your-huggingface-key' # if using Hugging Face
```

4. Start the backend:

```bash
./gradlew :runtime:self-hosted:bootRun
```

5. Start the [UI repository](https://github.com/Jordan890/cooksyne_ui) and authenticate.
6. Use `/settings/runtime` in the UI to configure AI provider/models.

### Build and Test Commands

```bash
./gradlew compileJava                          # Compile all modules
./gradlew test                                 # Run tests
./gradlew :runtime:self-hosted:bootRun         # Run the backend
```

### Building Docker Images Locally

```bash
# Backend (from cooksyne root)
docker build -t ghcr.io/jordan890/cooksyne-api:local .

# Frontend (from cooksyne_ui/cooksyne-ui)
docker build -t ghcr.io/jordan890/cooksyne-ui:local .
```

Then set in `.env`:

```bash
COOKSYNE_VERSION=local
```

### Repository Structure

- `core` — Domain logic
- `runtime:self-hosted` — Spring Boot application
- `adapters:persistence-jpa` — Database layer
- `adapters:auth-local` — Local auth adapter
- `adapters:ai-ollama` / `ai-openai` / `ai-bedrock` / `ai-huggingface` — AI providers

---

## Configuration Reference

### Environment Variables

| Variable                        | Purpose                          | Default                                          |
| ------------------------------- | -------------------------------- | ------------------------------------------------ |
| `DB_URL`                        | JDBC connection string           | `jdbc:postgresql://localhost:5432/cooksyne` |
| `DB_USERNAME`                   | Database user                    | `postgres`                                       |
| `DB_PASSWORD`                   | Database password                | (empty)                                          |
| `OAUTH2_ISSUER_URI`             | JWT issuer for token validation  | `http://localhost:8080/realms/cooksyne`     |
| `PORT`                          | Backend HTTP port                | `8081`                                           |
| `CORS_ALLOWED_ORIGINS`          | Comma-separated allowed origins  | `http://localhost:4200,http://localhost:9090`    |
| `OPENAI_API_KEY`                | OpenAI API key                   | (empty)                                          |
| `HUGGINGFACE_API_KEY`           | Hugging Face API key             | (empty)                                          |
| `COOKSYNE_STORAGE_IMAGE_DIR` | Override image storage directory | `${user.home}/.cooksyne/images`               |

### Runtime Configuration (UI-Editable)

These are managed via `/settings/runtime` in the UI and persisted per-user:

- `aiProvider` — `ollama`, `openai`, `bedrock`, or `huggingface`
- `ollamaBaseUrl` — default: `http://localhost:11434`
- `ollamaModel` — default: `llava-phi3`
- `openAiModel` — default: `gpt-4o-mini`
- `awsRegion` — default: `us-east-1`
- `bedrockModelId` — default: `anthropic.claude-3-haiku-20240307-v1:0`
- `huggingFaceModel` — default: `Salesforce/blip-image-captioning-large`

Core settings (DB, auth, port, API keys) are **not** editable from the UI — they require an env var change and restart.

---

## API Reference

### Recipes

- `GET /recipes` — List user's recipes
- `GET /recipes/{id}` — Get recipe by ID
- `POST /recipes` — Create or update recipe
- `DELETE /recipes/{id}` — Delete recipe

### Grocery Lists

- `GET /grocery_list` — List user's grocery lists
- `GET /grocery_list/{id}` — Get grocery list by ID
- `POST /grocery_list` — Create or update grocery list
- `DELETE /grocery_list/{id}` — Delete grocery list

### Images

- `POST /api/images` — Upload image (authenticated)
- `GET /api/images/{userId}/{filename}` — Get image (public)

### AI Analysis

- `POST /api/ai/analyze-food` — Analyze food from image
- `POST /api/ai/analyze-recipe` — Extract recipe from image

### Runtime Config

- `GET /config/runtime` — Get current runtime config
- `PUT /config/runtime` — Update runtime config

All endpoints require authentication except `/public/**`, `GET /api/images/**`, and `GET /actuator/health`.

---

## Authentication

- OAuth2 JWT resource server validates tokens against the configured issuer
- Users are auto-provisioned on first login (if email is verified)
- CORS defaults: `http://localhost:4200`, `http://localhost:9090` (configurable via `CORS_ALLOWED_ORIGINS`)

---

## Troubleshooting

### Startup Fails with DB Connection Errors

- Verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are correct and exported.
- Confirm PostgreSQL is running and reachable.

### 401 Unauthorized from API

- Verify JWT issuer matches `OAUTH2_ISSUER_URI`.
- Confirm the request includes a valid bearer token.

### Core Runtime Setting Changes Not Applying from UI

- Expected — core settings (DB, auth, port) are env vars. Update and restart the backend.

### Images Not Loading

- Confirm image URL format: `/api/images/{userId}/{filename}`.
- Confirm image file exists under the storage directory.

---

## Production Readiness Notes

1. Provide API keys with environment variables for any AI provider that requires them.
2. Use managed secret delivery for DB/API credentials.
3. Disable overly verbose SQL logging if not needed.
4. Enable TLS via the reverse proxy config or a load balancer.
5. Restrict CORS origins to trusted domains only — set `CORS_ALLOWED_ORIGINS` in `.env`.
6. Consider switching `spring.jpa.hibernate.ddl-auto` from `update` to `validate` and using a migration tool (Flyway/Liquibase) for schema management.
