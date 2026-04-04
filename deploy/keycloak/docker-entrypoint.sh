#!/bin/bash
# ─────────────────────────────────────────────────────────────────────
# Keycloak entrypoint wrapper
#
# Copies the baked-in CA cert + JKS truststore to a shared volume
# (/opt/keycloak/shared-tls) so that other containers (e.g. backend)
# can mount and trust Keycloak's self-signed TLS certificate.
#
# Then delegates to the standard Keycloak entrypoint.
# ─────────────────────────────────────────────────────────────────────
set -e

SHARED_TLS_DIR="/opt/keycloak/shared-tls"

if [ -d "$SHARED_TLS_DIR" ]; then
  echo "[keycloak-entrypoint] Copying TLS artifacts to shared volume ..."
  cp /opt/keycloak/conf/tls/ca.pem    "$SHARED_TLS_DIR/ca.pem"
  cp /opt/keycloak/conf/tls/truststore.jks "$SHARED_TLS_DIR/truststore.jks"
  echo "[keycloak-entrypoint] TLS artifacts ready."
fi

# Delegate to the original Keycloak entrypoint
exec /opt/keycloak/bin/kc.sh "$@"
