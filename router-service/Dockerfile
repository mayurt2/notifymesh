FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY vendor-adapters ./vendor-adapters
COPY ingestion-service ./ingestion-service
COPY router-service ./router-service
COPY audit-service ./audit-service
COPY query-service ./query-service
RUN mvn -q -pl vendor-adapters,router-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/router-service/target/router-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
