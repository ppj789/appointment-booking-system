FROM maven:3.10.0-eclipse-temurin-25-resolute AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src src
COPY config config
RUN mvn --batch-mode --no-transfer-progress verify \
    && cp "$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' -print -quit)" /workspace/app.jar

FROM eclipse-temurin:25-jre-resolute

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && groupadd --system booking \
    && useradd --system --gid booking --home-dir /app booking \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

USER booking

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
