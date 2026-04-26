#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
# Cooksyne — Setup Script
# ─────────────────────────────────────────────────────────────────────
# Generates secrets, creates .env, builds images, and starts the stack.
#
# Usage:
#   chmod +x setup.sh
#   ./setup.sh
# ─────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── Helpers ───────────────────────────────────────────────────────────

generate_password() {
  # Avoid SIGPIPE from `tr | head` under `set -o pipefail` by generating
  # a fixed-width hex token directly from /dev/urandom.
  LC_ALL=C od -An -N16 -tx1 /dev/urandom | tr -d ' \n'
}

prompt() {
  local var_name="$1" prompt_text="$2" default_value="${3:-}"
  local current_value
  current_value="$(grep "^${var_name}=" .env 2>/dev/null | cut -d= -f2- || true)"

  if [ "$var_name" = "JWT_PUBLIC_KEY" ] && [ "$current_value" = "PLACEHOLDER_FOR_BUILD" ]; then
    current_value=""
  fi

  if [ -n "$current_value" ]; then
    printf "  %s [%s]: " "$prompt_text" "$current_value"
    read -r input
    eval "$var_name=\"${input:-$current_value}\""
  elif [ -n "$default_value" ]; then
    printf "  %s [%s]: " "$prompt_text" "$default_value"
    read -r input
    eval "$var_name=\"${input:-$default_value}\""
  else
    printf "  %s: " "$prompt_text"
    read -r input
    eval "$var_name=\"$input\""
  fi
}

prompt_yn() {
  local var_name="$1" prompt_text="$2" default_value="${3:-y}"
  printf "  %s [%s]: " "$prompt_text" "$default_value"
  read -r input
  input="${input:-$default_value}"
  case "$input" in
    [Yy]*) eval "$var_name=y" ;;
    *)     eval "$var_name=n" ;;
  esac
}

# ── Banner ────────────────────────────────────────────────────────────

echo ""
echo "============================================="
echo "   Cooksyne — Setup"
echo "============================================="
echo ""

# ── Create .env if missing ────────────────────────────────────────────

if [ ! -f .env ]; then
  echo "Creating .env from .env.example..."
  cp .env.example .env
  echo ""
fi

# ── Component Selection ───────────────────────────────────────────────

echo "── Optional Components ──"
echo ""
echo "  Cooksyne includes optional bundled services."
echo "  You can skip any of these if you provide your own."
echo ""

prompt_yn USE_KEYCLOAK "Use bundled Keycloak for authentication? (y/n)" "y"
prompt_yn USE_CADDY    "Use bundled Caddy + Tailscale reverse proxy? (y/n)" "y"
prompt_yn USE_OLLAMA   "Use bundled Ollama for local AI? (y/n)" "y"

# Build COMPOSE_PROFILES
COMPOSE_PROFILES=""
[ "$USE_KEYCLOAK" = "y" ] && COMPOSE_PROFILES="${COMPOSE_PROFILES:+$COMPOSE_PROFILES,}keycloak"
[ "$USE_CADDY" = "y" ]    && COMPOSE_PROFILES="${COMPOSE_PROFILES:+$COMPOSE_PROFILES,}caddy"
[ "$USE_OLLAMA" = "y" ]   && COMPOSE_PROFILES="${COMPOSE_PROFILES:+$COMPOSE_PROFILES,}ollama"

echo ""

# ── Domain / URL Configuration ────────────────────────────────────────

echo "── Domain ──"
echo ""

if [ "$USE_CADDY" = "y" ]; then
  echo "  The app will be available at https://<hostname>.<tailnet>.ts.net"
  echo ""
  prompt TS_HOSTNAME "Tailscale machine name (e.g. cooksyne)" "cooksyne"
  prompt TS_AUTHKEY  "Tailscale auth key (from https://login.tailscale.com/admin/settings/keys)" ""
  echo ""
  echo "  Full domain: <TS_HOSTNAME>.<tailnet-name>.ts.net"
  echo "  Find your tailnet name at https://login.tailscale.com/admin/machines"
  echo ""
  prompt DOMAIN "Full domain (e.g. ${TS_HOSTNAME}.your-tailnet.ts.net)" ""
else
  TS_HOSTNAME=""
  TS_AUTHKEY=""
  echo "  Enter the domain where your reverse proxy serves the app."
  echo ""
  prompt DOMAIN "Domain (e.g. cook.example.com)" ""
fi

# Compute derived URLs (user can override later by editing .env)
PROTOCOL="https"
API_URL="${PROTOCOL}://${DOMAIN}/api"
CORS_ALLOWED_ORIGINS="${PROTOCOL}://${DOMAIN}"

if [ "$USE_KEYCLOAK" = "y" ]; then
  OAUTH2_ISSUER_URI="${PROTOCOL}://${DOMAIN}/auth/realms/cooksyne"
  AUTH_AUTHORITY="${PROTOCOL}://${DOMAIN}/auth/realms/cooksyne"
else
  echo ""
  echo "── External Auth Server ──"
  echo ""
  echo "  You'll need the OIDC issuer URI and client ID from your provider."
  echo "  Examples:"
  echo "    Auth0:  https://<tenant>.auth0.com/"
  echo "    Okta:   https://<org>.okta.com/oauth2/default"
  echo ""
  prompt OAUTH2_ISSUER_URI "OIDC issuer URI" ""
  AUTH_AUTHORITY="$OAUTH2_ISSUER_URI"
fi

echo ""
echo "── Database ──"
echo ""
DB_PASSWORD_DEFAULT="$(generate_password)"
prompt DB_USERNAME "Database username" "postgres"
prompt DB_PASSWORD "Database password (auto-generated)" "$DB_PASSWORD_DEFAULT"

if [ "$USE_KEYCLOAK" = "y" ]; then
  echo ""
  echo "── Keycloak ──"
  echo ""
  KC_PASSWORD_DEFAULT="$(generate_password)"
  prompt KEYCLOAK_ADMIN_USERNAME "Keycloak admin username" "admin"
  prompt KEYCLOAK_ADMIN_PASSWORD "Keycloak admin password (auto-generated)" "$KC_PASSWORD_DEFAULT"
else
  KEYCLOAK_ADMIN_USERNAME=""
  KEYCLOAK_ADMIN_PASSWORD=""
fi

echo ""
echo "── Image Version ──"
echo ""
prompt COOKSYNE_VERSION "Image tag" "release"

echo ""
echo "── JWT Public Key (optional — can set later) ──"
echo ""
if [ "$USE_KEYCLOAK" = "y" ]; then
  echo "  If this is your first launch, leave blank."
  echo "  After configuring Keycloak, run: ./setup.sh"
  echo "  to update JWT_PUBLIC_KEY."
else
  echo "  Paste the base64-encoded X.509 signing certificate from your auth provider."
  echo "  (No PEM headers — just the base64 string.)"
fi
echo ""
prompt JWT_PUBLIC_KEY "JWT public key (base64 X.509 certificate)" ""

if [ "$JWT_PUBLIC_KEY" = "PLACEHOLDER_FOR_BUILD" ]; then
  JWT_PUBLIC_KEY=""
fi

echo ""
echo "── Auth Client ──"
echo ""
prompt AUTH_CLIENT_ID "OIDC client ID" "cooksyne-ui"

# ── AI Configuration ─────────────────────────────────────────────────

echo ""
echo "── AI Configuration ──"
echo ""
echo "  AI powers recipe import from images/URLs. Provider and model"
echo "  can also be changed at runtime in the app (Settings > Runtime)."
echo ""
echo "  Providers:"
echo "    ollama       — Local AI via bundled Ollama (default, free)"
echo "    openai       — OpenAI API (requires API key)"
echo "    huggingface  — Hugging Face API (requires API key)"
echo "    bedrock      — AWS Bedrock (requires AWS credentials)"
echo "    disabled     — No AI features"
echo ""
prompt AI_PROVIDER "AI provider" "ollama"

OPENAI_API_KEY=""
HUGGINGFACE_API_KEY=""

case "$AI_PROVIDER" in
  openai)
    prompt OPENAI_API_KEY "OpenAI API key" ""
    ;;
  huggingface)
    prompt HUGGINGFACE_API_KEY "Hugging Face API key" ""
    ;;
  bedrock)
    echo "  AWS Bedrock uses default AWS credentials (env vars, instance role, etc.)."
    echo "  Set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_REGION separately."
    ;;
  ollama)
    if [ "$USE_OLLAMA" != "y" ]; then
      echo "  Note: You selected Ollama as AI provider but didn't enable the bundled"
      echo "  Ollama container. Make sure OLLAMA_BASE_URL points to your Ollama instance."
    fi
    ;;
esac

# Ollama settings (always write defaults — they're harmless if unused)
OLLAMA_BASE_URL_DEFAULT="http://ollama:11434"
[ "$USE_OLLAMA" != "y" ] && OLLAMA_BASE_URL_DEFAULT="http://host.docker.internal:11434"
current_ollama_url="$(grep "^OLLAMA_BASE_URL=" .env 2>/dev/null | cut -d= -f2- || true)"
OLLAMA_BASE_URL="${current_ollama_url:-$OLLAMA_BASE_URL_DEFAULT}"

# ── Write .env ────────────────────────────────────────────────────────

cat > .env <<EOF
# ── Compose Profiles ──────────────────────────────────────────────────
COMPOSE_PROFILES=${COMPOSE_PROFILES}

# ── Domain ────────────────────────────────────────────────────────────
DOMAIN=${DOMAIN}

# ── Application URLs ─────────────────────────────────────────────────
API_URL=${API_URL}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
OAUTH2_ISSUER_URI=${OAUTH2_ISSUER_URI}
AUTH_AUTHORITY=${AUTH_AUTHORITY}

# ── Image version ────────────────────────────────────────────────────
COOKSYNE_VERSION=${COOKSYNE_VERSION}

# ── Database ──────────────────────────────────────────────────────────
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# ── JWT ───────────────────────────────────────────────────────────────
JWT_PUBLIC_KEY=${JWT_PUBLIC_KEY}

# ── UI ────────────────────────────────────────────────────────────────
AUTH_CLIENT_ID=${AUTH_CLIENT_ID}

# ── Keycloak ──────────────────────────────────────────────────────────
KEYCLOAK_ADMIN_USERNAME=${KEYCLOAK_ADMIN_USERNAME}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}

# ── Tailscale / Caddy ────────────────────────────────────────────────
TS_HOSTNAME=${TS_HOSTNAME}
TS_AUTHKEY=${TS_AUTHKEY}

# ── AI ────────────────────────────────────────────────────────────────
AI_PROVIDER=${AI_PROVIDER}
OPENAI_API_KEY=${OPENAI_API_KEY}
HUGGINGFACE_API_KEY=${HUGGINGFACE_API_KEY}

# ── Ollama ────────────────────────────────────────────────────────────
OLLAMA_BASE_URL=${OLLAMA_BASE_URL}
OLLAMA_NUM_CTX=2048
OLLAMA_NUM_THREAD=4
OLLAMA_NUM_PREDICT=512
EOF

echo ""
echo ".env written."
echo ""

# ── Build Caddy image (if using Caddy) ────────────────────────────────

if [ "$USE_CADDY" = "y" ]; then
  echo "Building Caddy image (with Tailscale plugin)..."
  docker compose build caddy
  echo ""
fi

# ── Pull application images ───────────────────────────────────────────

echo "Pulling application images..."
docker compose pull --ignore-buildable
echo ""

# ── Start or inform ──────────────────────────────────────────────────

if [ "$USE_KEYCLOAK" = "y" ] && [ -z "$JWT_PUBLIC_KEY" ]; then
  echo "============================================="
  echo "   Phase 1 Complete — Keycloak Setup Needed"
  echo "============================================="
  echo ""
  echo "JWT_PUBLIC_KEY is not set yet. Starting Keycloak and"
  echo "PostgreSQL so you can configure the realm."
  echo ""

  docker compose up -d postgres keycloak
  [ "$USE_CADDY" = "y" ] && docker compose up -d caddy
  echo ""
  echo "Wait for Keycloak to start, then:"
  echo ""
  if [ "$USE_CADDY" = "y" ]; then
    echo "  1. Open https://${DOMAIN}/auth/"
  else
    echo "  1. Open Keycloak via your reverse proxy at /auth/"
    echo "     (or directly at http://localhost:8080 if ports are exposed)"
  fi
  echo ""
  echo "  2. Log in:  ${KEYCLOAK_ADMIN_USERNAME} / ${KEYCLOAK_ADMIN_PASSWORD}"
  echo "  3. Create realm: cooksyne"
  echo "  4. Create client: ${AUTH_CLIENT_ID}"
  echo "       - Client type: OpenID Connect"
  echo "       - Valid redirect URIs: https://${DOMAIN}/*"
  echo "       - Web origins: https://${DOMAIN}"
  echo "  5. Create a user and set a password"
  echo "  6. Get the JWT certificate:"
  echo "       Realm Settings > Keys > RS256 row > Certificate"
  echo "  7. Re-run this script and paste the certificate:"
  echo "       ./setup.sh"
  echo ""
elif [ -z "$JWT_PUBLIC_KEY" ]; then
  echo "============================================="
  echo "   Almost Ready — JWT Key Needed"
  echo "============================================="
  echo ""
  echo "JWT_PUBLIC_KEY is not set. Get the RS256 signing certificate"
  echo "from your auth provider and re-run:"
  echo "  ./setup.sh"
  echo ""
else
  echo "============================================="
  echo "   Starting Cooksyne"
  echo "============================================="
  echo ""
  docker compose up -d
  echo ""
  echo "All services starting. Access the app at:"
  echo ""
  echo "  https://${DOMAIN}"
  echo ""
  if [ "$USE_KEYCLOAK" = "y" ]; then
    echo "Keycloak admin:"
    echo "  https://${DOMAIN}/auth/"
    echo ""
  fi
  if [ "$AI_PROVIDER" = "ollama" ]; then
    echo "Note: You'll need to pull an Ollama model before AI features work:"
    echo "  docker compose exec ollama ollama pull qwen2.5:1.5b"
    echo ""
  fi
fi
