FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY shared-kernel shared-kernel
COPY services services
RUN mvn -pl services/risk-service -am -DskipTests package
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 novabank
WORKDIR /app
COPY --from=build /workspace/services/risk-service/target/risk-service-*.jar app.jar
USER 10001
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/app.jar"]
