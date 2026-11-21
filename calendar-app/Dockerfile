# Build context is the repository root (multi-module Maven project)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY core/pom.xml core/
COPY booking-app/pom.xml booking-app/
COPY calendar-app/pom.xml calendar-app/
RUN mvn -B -q -pl calendar-app -am dependency:go-offline
COPY core/src core/src
COPY calendar-app/src calendar-app/src
RUN mvn -B -q -pl calendar-app -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/calendar-app/target/calendar-app-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
