FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY vendor-adapters ./vendor-adapters
COPY ingestion-service ./ingestion-service
COPY router-service ./router-service
COPY audit-service ./audit-service
COPY query-service ./query-service
RUN mvn -q -pl vendor-adapters,query-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/query-service/target/query-service.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
