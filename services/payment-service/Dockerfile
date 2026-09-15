FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY shared-kernel ./shared-kernel
COPY services ./services
RUN mvn -q -pl services/payment-service -am package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
LABEL org.opencontainers.image.title="NovaBank payment-service" org.opencontainers.image.source="https://github.com/tharunktr29/NovaBanking-project"
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 novabank
COPY --from=build /workspace/services/payment-service/target/payment-service-*.jar app.jar
RUN chown 10001:0 app.jar && chmod 0440 app.jar
USER 10001
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl -fsS http://localhost:8086/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
