FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:integration-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/integration-service/build/libs/integration-service-0.1.0.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java", "-jar", "app.jar"]
