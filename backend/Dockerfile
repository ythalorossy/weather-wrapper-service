# syntax=docker/dockerfile:1.7

# =====================================================================
# Stage 1: Build with Maven
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve and cache dependencies first so this layer is reused across builds
# when only source code changes.
COPY pom.xml ./
COPY weather-domain/pom.xml          weather-domain/
COPY weather-application/pom.xml     weather-application/
COPY weather-infrastructure/pom.xml  weather-infrastructure/
COPY weather-api/pom.xml             weather-api/
RUN mvn -B -ntp -e -q dependency:go-offline

# Copy sources and build (skip tests; the test stage runs them in CI)
COPY weather-domain/src          weather-domain/src
COPY weather-application/src     weather-application/src
COPY weather-infrastructure/src  weather-infrastructure/src
COPY weather-api/src             weather-api/src
RUN mvn -B -ntp -e -DskipTests package

# =====================================================================
# Stage 2: Minimal runtime (JRE only, non-root)
# =====================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/weather-api/target/*.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]