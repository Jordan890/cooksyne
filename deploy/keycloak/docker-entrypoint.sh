#!/usr/bin/env bash
set -euo pipefail

realm_template="/opt/keycloak/data/import/cooksyne-realm.template.json"
realm_output="/opt/keycloak/data/import/cooksyne-realm.json"

: "${DOMAIN:?DOMAIN is required for Keycloak realm import}"
: "${AUTH_CLIENT_ID:=cooksyne-ui}"

escape_sed_replacement() {
	printf '%s' "$1" | sed -e 's/[\\&|]/\\\\&/g'
}

domain_replacement="$(escape_sed_replacement "$DOMAIN")"
client_id_replacement="$(escape_sed_replacement "$AUTH_CLIENT_ID")"

sed \
	-e "s|\${DOMAIN}|$domain_replacement|g" \
	-e "s|\${AUTH_CLIENT_ID}|$client_id_replacement|g" \
	"$realm_template" > "$realm_output"

exec /opt/keycloak/bin/kc.sh "$@"