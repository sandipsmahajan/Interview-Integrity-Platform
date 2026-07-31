FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:storage-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/storage-service/build/libs/storage-service-0.1.0.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
