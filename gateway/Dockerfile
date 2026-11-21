# Build context is the repository root (multi-module Maven project)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY core/pom.xml core/
COPY booking-app/pom.xml booking-app/
COPY calendar-app/pom.xml calendar-app/
COPY gateway/pom.xml gateway/
RUN mvn -B -q -pl gateway -am dependency:go-offline
COPY gateway/src gateway/src
RUN mvn -B -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/gateway/target/gateway-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
