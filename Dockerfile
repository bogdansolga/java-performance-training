# Java Performance Training — lab image
#
# Multi-stage build: compile with Maven + JDK 21, ship only the JRE + jar.
# Published by the trainer as bogdansolga/java-perf-training; participants pull it.
# Built multi-arch because the trainer is on arm64 and participants are on amd64.
#
#   docker buildx build --platform linux/amd64,linux/arm64 \
#     -t bogdansolga/java-perf-training --push .

# ---- build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so edits to src/ don't re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
