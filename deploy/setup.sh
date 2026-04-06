#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
# Cart & Cook — Setup Script
# ─────────────────────────────────────────────────────────────────────
# Generates secrets, creates .env, builds the Caddy image, pulls
# container images, and starts the stack.
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
  LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32
}

prompt() {
  local var_name="$1" prompt_text="$2" default_value="${3:-}"
  local current_value
  current_value="$(grep "^${var_name}=" .env 2>/dev/null | cut -d= -f2- || true)"

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

# ── Banner ────────────────────────────────────────────────────────────

echo ""
echo "============================================="
echo "   Cart & Cook — Setup"
echo "============================================="
echo ""

# ── Create .env if missing ────────────────────────────────────────────

if [ ! -f .env ]; then
  echo "Creating .env from .env.example..."
  cp .env.example .env
  echo ""
fi

# ── Collect configuration ─────────────────────────────────────────────

echo "── Tailscale Configuration ──"
echo ""
echo "  The app will be available at https://<hostname>.<tailnet>.ts.net"
echo ""
prompt TS_HOSTNAME "Tailscale machine name (e.g. cart-and-cook)" "cart-and-cook"
prompt TS_AUTHKEY  "Tailscale auth key (from https://login.tailscale.com/admin/settings/keys)" ""

echo ""
echo "── Domain ──"
echo ""
echo "  Full domain name: <TS_HOSTNAME>.<tailnet-name>.ts.net"
echo "  Find your tailnet name at https://login.tailscale.com/admin/machines"
echo ""
prompt DOMAIN "Full domain (e.g. ${TS_HOSTNAME}.your-tailnet.ts.net)" ""

echo ""
echo "── Database ──"
echo ""
DB_PASSWORD_DEFAULT="$(generate_password)"
prompt DB_USERNAME "Database username" "postgres"
prompt DB_PASSWORD "Database password (auto-generated)" "$DB_PASSWORD_DEFAULT"

echo ""
echo "── Keycloak ──"
echo ""
KC_PASSWORD_DEFAULT="$(generate_password)"
prompt KEYCLOAK_ADMIN_USERNAME "Keycloak admin username" "admin"
prompt KEYCLOAK_ADMIN_PASSWORD "Keycloak admin password (auto-generated)" "$KC_PASSWORD_DEFAULT"

echo ""
echo "── Image Version ──"
echo ""
prompt CART_AND_COOK_VERSION "Image tag" "release"

echo ""
echo "── JWT Public Key (optional — can set later) ──"
echo ""
echo "  If this is your first launch, leave blank."
echo "  After configuring Keycloak, run: ./setup.sh"
echo "  to update JWT_PUBLIC_KEY."
echo ""
prompt JWT_PUBLIC_KEY "JWT public key (base64 X.509 certificate)" ""

echo ""
echo "── Auth Client ──"
echo ""
prompt AUTH_CLIENT_ID "Keycloak OIDC client ID" "cart-and-cook-ui"

# ── Write .env ────────────────────────────────────────────────────────

cat > .env <<EOF
# ── Tailscale ─────────────────────────────────────────────────────────
TS_HOSTNAME=${TS_HOSTNAME}
TS_AUTHKEY=${TS_AUTHKEY}

# ── Domain ────────────────────────────────────────────────────────────
DOMAIN=${DOMAIN}

# ── Image version ────────────────────────────────────────────────────
CART_AND_COOK_VERSION=${CART_AND_COOK_VERSION}

# ── Database ──────────────────────────────────────────────────────────
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

# ── Keycloak ──────────────────────────────────────────────────────────
KEYCLOAK_ADMIN_USERNAME=${KEYCLOAK_ADMIN_USERNAME}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}

# ── JWT ───────────────────────────────────────────────────────────────
JWT_PUBLIC_KEY=${JWT_PUBLIC_KEY}

# ── UI ────────────────────────────────────────────────────────────────
AUTH_CLIENT_ID=${AUTH_CLIENT_ID}

# ── AI providers (optional) ──────────────────────────────────────────
# OPENAI_API_KEY=
# HUGGINGFACE_API_KEY=
# OLLAMA_BASE_URL=http://host.docker.internal:11434
EOF

echo ""
echo ".env written."
echo ""

# ── Build Caddy image ─────────────────────────────────────────────────

echo "Building Caddy image (with Tailscale plugin)..."
docker compose build caddy
echo ""

# ── Pull application images ───────────────────────────────────────────

echo "Pulling application images..."
docker compose pull --ignore-buildable
echo ""

# ── Start or inform ──────────────────────────────────────────────────

if [ -z "$JWT_PUBLIC_KEY" ]; then
  echo "============================================="
  echo "   Phase 1 Complete — Keycloak Setup Needed"
  echo "============================================="
  echo ""
  echo "JWT_PUBLIC_KEY is not set yet. Starting Keycloak and"
  echo "PostgreSQL so you can configure the realm."
  echo ""

  docker compose up -d postgres keycloak
  echo ""
  echo "Wait for Keycloak to start, then:"
  echo ""
  echo "  1. Open https://${DOMAIN}/auth/"
  echo "     (or http://localhost:8080/auth/ if Caddy is not yet running)"
  echo ""
  echo "     Since Caddy is not up yet, start it first to get the"
  echo "     Tailscale hostname working, OR access Keycloak directly:"
  echo "     docker compose up -d caddy"
  echo ""
  echo "  2. Log in:  ${KEYCLOAK_ADMIN_USERNAME} / ${KEYCLOAK_ADMIN_PASSWORD}"
  echo "  3. Create realm: cart_and_cook"
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
else
  echo "============================================="
  echo "   Starting Cart & Cook"
  echo "============================================="
  echo ""
  docker compose up -d
  echo ""
  echo "All services starting. Access the app at:"
  echo ""
  echo "  https://${DOMAIN}"
  echo ""
  echo "Keycloak admin:"
  echo "  https://${DOMAIN}/auth/"
  echo ""
fi
