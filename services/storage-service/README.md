# storage-service

storage service (object metadata, signed URLs).

## Technologies

- Java 21, Spring Boot 4.1 (WebFlux), Spring Cloud 2025.1, Kafka, PostgreSQL 15

## Quick start

Prerequisites: JDK 21, PostgreSQL on localhost:5432, Kafka on localhost:9092.

    ./gradlew :services:storage-service:bootRun

Health: http://localhost:8093/actuator/health
OpenAPI: http://localhost:8093/swagger-ui.html

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `server.port` | 8093 | HTTP port |
| `spring.r2dbc.url` | r2dbc:postgresql://localhost:5432/storage_db | Database connection |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Event bus |

## Database

Owned schema in `storage_db`, managed by Flyway migrations under
`src/main/resources/db/migration` (`V1__init_schema.sql`,
`R__reference_data.sql`). No other service writes to this database.

## API

Exposed under `/api/v1`, documented by OpenAPI. Errors follow the platform
`ErrorResponse` contract.

## Architecture decisions

See `ADR.md`.
