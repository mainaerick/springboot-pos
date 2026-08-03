# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY config/ config/

RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/

ARG SKIP_TESTS=false

RUN if [ "$SKIP_TESTS" = "true" ]; then \
        ./mvnw -q -DskipTests package; \
    else \
        ./mvnw -q clean verify; \
    fi

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --create-home --home-dir /home/spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
