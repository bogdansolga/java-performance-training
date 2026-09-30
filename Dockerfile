# Java Performance Training - lab image
#
# Multi-stage build: compile with Maven + JDK 21, run on a full JDK so the profiling tools are inside.
# Published by the trainer as bogdansolga/java-perf-training; participants pull it.
# Built multi-arch because the trainer is on arm64 and participants are on amd64.
#
#   docker buildx build --platform linux/amd64,linux/arm64 \
#     -t bogdansolga/java-perf-training --push .
#
# Profiling the running container
#   - JDK tools (jcmd, jfr, jstack) are on the PATH:
#       docker exec <container> jcmd 1 Thread.print
#       docker exec <container> jcmd 1 JFR.start duration=60s filename=/tmp/app.jfr
#   - async-profiler is on the PATH (use the itimer engine - containers usually block perf events):
#       docker exec <container> asprof -e itimer -d 30 -f /tmp/flame.html 1
#   - JMX for VisualVM / JMC: start with JMX=1 and publish 9010, then connect to localhost:9010:
#       docker run -e JMX=1 -p 8080:8080 -p 9010:9010 bogdansolga/java-perf-training
#   - Extra JVM options: JAVA_OPTS, e.g. -e JAVA_OPTS="-Xmx256m -XX:+UseG1GC"
#   Copy a recording out with: docker cp <container>:/tmp/app.jfr .

# ---- build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so edits to src/ don't re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- async-profiler -----------------------------------------------------
FROM alpine:3 AS profiler
ARG TARGETARCH
ARG ASYNC_PROFILER_VERSION=4.5
RUN arch=$([ "$TARGETARCH" = "arm64" ] && echo arm64 || echo x64) \
 && wget -qO- "https://github.com/async-profiler/async-profiler/releases/download/v${ASYNC_PROFILER_VERSION}/async-profiler-${ASYNC_PROFILER_VERSION}-linux-${arch}.tar.gz" \
    | tar -xz -C /opt \
 && mv /opt/async-profiler-* /opt/async-profiler

# ---- runtime stage -------------------------------------------------------
# A JDK (not a JRE), glibc-based: jcmd, jfr and jstack need the JDK, async-profiler needs glibc.
FROM eclipse-temurin:21-jdk
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=profiler /opt/async-profiler /opt/async-profiler
COPY --from=build /build/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV PATH="/opt/async-profiler/bin:${PATH}" \
    JAVA_OPTS="" \
    JMX_FLAGS="-Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 \
-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=127.0.0.1"

EXPOSE 8080 9010

# JMX is off unless JMX is set (unauthenticated JMX - lab use only). Arguments after the image name reach the app.
ENTRYPOINT ["sh", "-c", "exec java ${JMX:+$JMX_FLAGS} $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
