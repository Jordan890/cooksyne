#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
# Cart & Cook – Self-Hosted Setup
# Usage:  ./setup.sh [--no-start]
#
# Supports external Postgres and/or Keycloak — set USE_EXTERNAL_DB
# and/or USE_EXTERNAL_KEYCLOAK to "true" in .env after generation.
# ─────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
info() { echo -e "${CYAN}[i]${NC} $1"; }

# ── 1. Generate .env ─────────────────────────────────────────────────
if [ -f .env ]; then
  warn ".env already exists — skipping generation."
  info "Delete .env and re-run to regenerate."
else
  info "Creating .env from .env.example ..."
  cp .env.example .env

  # Generate random secrets
  DB_PASS="$(openssl rand -base64 24 | tr -d '/+=' | head -c 32)"
  KC_PASS="$(openssl rand -base64 24 | tr -d '/+=' | head -c 32)"

  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/DB_PASSWORD=CHANGE_ME/DB_PASSWORD=${DB_PASS}/" .env
    sed -i '' "s/KEYCLOAK_ADMIN_PASSWORD=CHANGE_ME/KEYCLOAK_ADMIN_PASSWORD=${KC_PASS}/" .env
  else
    sed -i "s/DB_PASSWORD=CHANGE_ME/DB_PASSWORD=${DB_PASS}/" .env
    sed -i "s/KEYCLOAK_ADMIN_PASSWORD=CHANGE_ME/KEYCLOAK_ADMIN_PASSWORD=${KC_PASS}/" .env
  fi

  log ".env created with generated secrets."
  info "Database password: ${DB_PASS}"
  info "Keycloak admin password: ${KC_PASS}"
  echo ""
  warn "Save these passwords! They won't be shown again."
  echo ""
fi

# ── 2. Load .env values ─────────────────────────────────────────────
set -a
# shellcheck disable=SC1091
source .env
set +a

USE_EXTERNAL_DB="${USE_EXTERNAL_DB:-false}"
USE_EXTERNAL_KEYCLOAK="${USE_EXTERNAL_KEYCLOAK:-false}"
USE_PROXY="${USE_PROXY:-false}"

# ── 3. Detect host IP for external URLs ──────────────────────────────
detect_host() {
  if command -v hostname &>/dev/null && hostname -I &>/dev/null 2>&1; then
    hostname -I | awk '{print $1}'
  elif command -v ipconfig &>/dev/null; then
    ipconfig getifaddr en0 2>/dev/null || echo "localhost"
  else
    echo "localhost"
  fi
}

HOST_IP="$(detect_host)"
info "Detected host IP: ${HOST_IP}"
info "If accessing from other devices, update API_URL and AUTH_AUTHORITY in .env"
info "to use ${HOST_IP} instead of localhost."

# ── 4. Check Docker ──────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
  echo ""
  warn "Docker is not installed. Please install Docker first:"
  info "  https://docs.docker.com/get-docker/"
  exit 1
fi

if ! docker info &>/dev/null 2>&1; then
  echo ""
  warn "Docker daemon is not running. Please start Docker."
  exit 1
fi

log "Docker is ready."

# ── 5. Build compose profiles ────────────────────────────────────────
PROFILES=()

if [[ "$USE_EXTERNAL_DB" == "true" ]]; then
  info "Using external database (USE_EXTERNAL_DB=true)"
  info "Make sure DB_URL, DB_USERNAME, and DB_PASSWORD point to your Postgres instance."
else
  PROFILES+=("db")
  info "Starting Docker-managed PostgreSQL"
fi

if [[ "$USE_EXTERNAL_KEYCLOAK" == "true" ]]; then
  info "Using external Keycloak (USE_EXTERNAL_KEYCLOAK=true)"
  info "Make sure OAUTH2_ISSUER_URI and AUTH_AUTHORITY point to your Keycloak instance."
else
  PROFILES+=("keycloak")
  info "Starting Docker-managed Keycloak"
fi

if [[ "$USE_PROXY" == "true" ]]; then
  PROFILES+=("proxy")
  info "Starting optional Caddy reverse proxy (USE_PROXY=true)"
fi

PROFILE_ARGS=""
for p in "${PROFILES[@]}"; do
  PROFILE_ARGS="${PROFILE_ARGS} --profile ${p}"
done

# ── 6. Start containers ─────────────────────────────────────────────
if [[ "${1:-}" == "--no-start" ]]; then
  info "Skipping container startup (--no-start flag)."
  echo ""
  log "Setup complete! Start with:"
  info "  docker compose${PROFILE_ARGS} up -d"
  exit 0
fi

echo ""
info "Building custom images (Keycloak TLS, Caddy Tailscale) ..."
# shellcheck disable=SC2086
docker compose${PROFILE_ARGS} build

echo ""
info "Pulling remaining images ..."
# shellcheck disable=SC2086
docker compose${PROFILE_ARGS} pull --ignore-buildable

echo ""
info "Starting containers ..."
# shellcheck disable=SC2086
docker compose${PROFILE_ARGS} up -d

echo ""
log "All containers started!"
echo ""
info "Services:"
info "  Frontend:  http://localhost:${FRONTEND_PORT:-3000}"
info "  Backend:   http://localhost:${BACKEND_PORT:-8081}"
if [[ "$USE_EXTERNAL_KEYCLOAK" != "true" ]]; then
  info "  Keycloak:  http://localhost:${KEYCLOAK_PORT:-8080}"
  info "  Keycloak (HTTPS): https://localhost:${KEYCLOAK_HTTPS_PORT:-8443}"
fi
if [[ "$USE_EXTERNAL_DB" != "true" ]]; then
  info "  Postgres:  localhost:${DB_PORT:-5432}"
fi
echo ""
if [[ "$USE_EXTERNAL_KEYCLOAK" != "true" ]]; then
  info "Next steps:"
  info "  1. Open Keycloak admin console and create the 'cart_and_cook' realm"
  info "  2. Create a client 'cart-and-cook-ui' with redirect URIs"
  info "  3. Create a user account"
  info "  4. Open the frontend and log in"
  echo ""
fi
info "Useful commands:"
info "  docker compose${PROFILE_ARGS} logs -f        # Follow logs"
info "  docker compose${PROFILE_ARGS} ps             # Service status"
info "  docker compose${PROFILE_ARGS} down           # Stop all"
info "  docker compose${PROFILE_ARGS} down -v        # Stop all + delete data"
