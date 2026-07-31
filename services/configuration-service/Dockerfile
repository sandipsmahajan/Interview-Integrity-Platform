FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :services:configuration-service:bootJar --no-daemon -x test -x check

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/configuration-service/build/libs/configuration-service-0.1.0.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "app.jar"]
