FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:feature-flag-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/feature-flag-service/build/libs/feature-flag-service-0.1.0.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "app.jar"]
