FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:candidate-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/services/candidate-service/build/libs/candidate-service-0.1.0.jar app.jar
USER appuser
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
