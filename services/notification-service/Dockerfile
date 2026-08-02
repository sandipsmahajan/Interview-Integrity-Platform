FROM gradle:9.3.0-jdk21 AS build
USER root
WORKDIR /workspace
COPY . .
RUN gradle :services:notification-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/services/notification-service/build/libs/notification-service-0.1.0.jar app.jar
USER appuser
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8090/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
