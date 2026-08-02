FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:interview-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/services/interview-service/build/libs/interview-service-0.1.0.jar app.jar
USER appuser
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
