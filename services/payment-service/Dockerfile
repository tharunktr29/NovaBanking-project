FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY shared-kernel ./shared-kernel
COPY services ./services
RUN mvn -q -pl services/payment-service -am package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/services/payment-service/target/payment-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
