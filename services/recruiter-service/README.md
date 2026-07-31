# recruiter-service

recruiter service (pipelines, requisitions).

## Technologies

- Java 21, Spring Boot 4.1 (WebFlux), Spring Cloud 2025.1, Kafka, PostgreSQL 15

## Quick start

Prerequisites: JDK 21, PostgreSQL on localhost:5432, Kafka on localhost:9092.

    ./gradlew :services:recruiter-service:bootRun

Health: http://localhost:8083/actuator/health
OpenAPI: http://localhost:8083/swagger-ui.html

## Configuration

| Key                              | Default                                        | Description         |
|----------------------------------|------------------------------------------------|---------------------|
| `server.port`                    | 8083                                           | HTTP port           |
| `spring.r2dbc.url`               | r2dbc:postgresql://localhost:5432/recruiter_db | Database connection |
| `spring.kafka.bootstrap-servers` | localhost:9092                                 | Event bus           |

## Database

Owned schema in `recruiter_db`, managed by Flyway migrations under
`src/main/resources/db/migration` (`V1__init_schema.sql`,
`R__reference_data.sql`). No other service writes to this database.

## API

Exposed under `/api/v1`, documented by OpenAPI. Errors follow the platform
`ErrorResponse` contract.

## Architecture decisions

See `ADR.md`.
