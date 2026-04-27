FROM quay.io/keycloak/keycloak:26.1

USER root

RUN microdnf install -y gettext \
    && microdnf clean all

COPY docker-entrypoint.sh /opt/keycloak/bin/cooksyne-entrypoint.sh
COPY realm-template.json /opt/keycloak/data/import/cooksyne-realm.template.json
COPY theme/cooksyne /opt/keycloak/themes/cooksyne

RUN chmod 0755 /opt/keycloak/bin/cooksyne-entrypoint.sh

USER 1000

ENTRYPOINT ["/opt/keycloak/bin/cooksyne-entrypoint.sh"]
CMD ["start", "--import-realm"]