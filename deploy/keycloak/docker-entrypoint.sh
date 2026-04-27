#!/usr/bin/env bash
set -euo pipefail

realm_template="/opt/keycloak/data/import/cooksyne-realm.template.json"
realm_output="/opt/keycloak/data/import/cooksyne-realm.json"

: "${DOMAIN:?DOMAIN is required for Keycloak realm import}"
: "${AUTH_CLIENT_ID:=cooksyne-ui}"

export DOMAIN AUTH_CLIENT_ID

envsubst '${DOMAIN} ${AUTH_CLIENT_ID}' < "$realm_template" > "$realm_output"

exec /opt/keycloak/bin/kc.sh "$@"