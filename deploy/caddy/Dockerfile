# ─────────────────────────────────────────────────────────────────────
# Custom Caddy image with the caddy-tailscale plugin
#
# Uses xcaddy to compile Caddy with the Tailscale transport module so
# Caddy can automatically obtain TLS certificates from Tailscale
# without needing manual OpenSSL cert generation.
#
# Ref: https://github.com/tailscale/caddy-tailscale
# ─────────────────────────────────────────────────────────────────────
FROM caddy:2-builder AS builder

RUN xcaddy build \
    --with github.com/tailscale/caddy-tailscale

# ── Final image ─────────────────────────────────────────────────
FROM caddy:2-alpine

COPY --from=builder /usr/bin/caddy /usr/bin/caddy

EXPOSE 80 443
