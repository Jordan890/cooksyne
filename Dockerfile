# ── Stage 1: Build ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts settings.gradle.kts ./
COPY core core
COPY adapters adapters
COPY runtime runtime

RUN chmod +x gradlew && ./gradlew :runtime:self-hosted:bootJar --no-daemon -x test

# ── Stage 2: Run ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

ARG APP_VERSION=0.0.0
LABEL org.opencontainers.image.title="cart-and-cook-api" \
    org.opencontainers.image.version="${APP_VERSION}" \
    org.opencontainers.image.description="Cart & Cook backend API"

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/runtime/self-hosted/build/libs/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
