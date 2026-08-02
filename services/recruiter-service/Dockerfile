FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:recruiter-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/services/recruiter-service/build/libs/recruiter-service-0.1.0.jar app.jar
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
