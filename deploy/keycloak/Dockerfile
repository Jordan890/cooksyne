# ─────────────────────────────────────────────────────────────────────
# Custom Keycloak image with a self-signed TLS certificate
#
# Keycloak serves HTTPS on port 8443 so backend services can validate
# JWT tokens over a TLS connection without external proxy help.
#
# The self-signed CA cert is exported to /opt/keycloak/conf/tls-ca.pem
# so consuming containers can mount and trust it.
# ─────────────────────────────────────────────────────────────────────
FROM quay.io/keycloak/keycloak:26.2 AS builder

USER root

# Generate a self-signed CA + server certificate at build time.
# The SAN list covers all names the backend or proxy might use to
# reach Keycloak inside the Docker network.
RUN microdnf install -y openssl && microdnf clean all && \
    mkdir -p /opt/keycloak/conf/tls && \
    # 1. Create a CA key + cert
    openssl genrsa -out /opt/keycloak/conf/tls/ca.key 4096 && \
    openssl req -new -x509 -days 3650 -key /opt/keycloak/conf/tls/ca.key \
    -out /opt/keycloak/conf/tls/ca.pem \
    -subj "/CN=Cart and Cook Internal CA" && \
    # 2. Create server key + CSR
    openssl genrsa -out /opt/keycloak/conf/tls/server.key 4096 && \
    openssl req -new -key /opt/keycloak/conf/tls/server.key \
    -out /opt/keycloak/conf/tls/server.csr \
    -subj "/CN=keycloak" && \
    # 3. Sign server cert with our CA (include SANs)
    printf "[v3_ext]\nsubjectAltName=DNS:keycloak,DNS:localhost,IP:127.0.0.1\n" \
    > /opt/keycloak/conf/tls/san.cnf && \
    openssl x509 -req -days 3650 \
    -in /opt/keycloak/conf/tls/server.csr \
    -CA /opt/keycloak/conf/tls/ca.pem \
    -CAkey /opt/keycloak/conf/tls/ca.key \
    -CAcreateserial \
    -out /opt/keycloak/conf/tls/server.pem \
    -extfile /opt/keycloak/conf/tls/san.cnf -extensions v3_ext && \
    # 4. Create a JKS truststore containing the CA cert (for Java-based consumers)
    keytool -importcert -noprompt -trustcacerts \
    -alias cart-and-cook-ca \
    -file /opt/keycloak/conf/tls/ca.pem \
    -keystore /opt/keycloak/conf/tls/truststore.jks \
    -storepass changeit && \
    # Clean up intermediate files
    rm -f /opt/keycloak/conf/tls/server.csr /opt/keycloak/conf/tls/san.cnf \
    /opt/keycloak/conf/tls/ca.srl && \
    chown -R 1000:0 /opt/keycloak/conf/tls

# ── Final image ─────────────────────────────────────────────────
FROM quay.io/keycloak/keycloak:26.2

COPY --from=builder /opt/keycloak/conf/tls /opt/keycloak/conf/tls
COPY docker-entrypoint.sh /opt/keycloak/docker-entrypoint.sh

USER root
RUN chmod +x /opt/keycloak/docker-entrypoint.sh && \
    chown 1000:0 /opt/keycloak/docker-entrypoint.sh
USER 1000

# Point Keycloak at the generated certs for HTTPS
ENV KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/tls/server.pem
ENV KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/tls/server.key

ENTRYPOINT ["/opt/keycloak/docker-entrypoint.sh"]

# Expose both HTTP and HTTPS
EXPOSE 8080 8443
