# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache Gradle deps separately from source for faster rebuilds
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

COPY src ./src
COPY opentelemetry-javaagent.jar ./opentelemetry-javaagent.jar

RUN ./gradlew bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create user, group, and pre-create logs directory with correct permissions
RUN groupadd -r tracepilot && useradd -r -g tracepilot tracepilot \
    && mkdir -p /app/logs \
    && chown -R tracepilot:tracepilot /app

# Use --chown to ensure the application user owns the runtime jars
COPY --from=build --chown=tracepilot:tracepilot /app/build/libs/*.jar app.jar
COPY --from=build --chown=tracepilot:tracepilot /app/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

ENV OTEL_SERVICE_NAME=tracepilot-api \
    OTEL_TRACES_EXPORTER=otlp \
    OTEL_TRACES_SAMPLER=always_on \
    OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
    OTEL_METRICS_EXPORTER=none \
    OTEL_LOGS_EXPORTER=none

EXPOSE 8080

USER tracepilot

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/opentelemetry-javaagent.jar -Dotel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:-http://jaeger:4317} -jar /app/app.jar"]