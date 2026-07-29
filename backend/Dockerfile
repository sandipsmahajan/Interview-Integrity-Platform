FROM gradle:8.12-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY config config
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY backend/src backend/src
RUN gradle --no-daemon :backend:bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
