# Interview Integrity Platform Architecture

The repository is an enterprise scaffold for a transparent, consent-driven interview integrity
platform, implemented as a set of reactive microservices sharing common libraries.

## Module Layout

- `libs`: shared libraries reused by every service.
  - `security` — HMAC-signed JWT issue/validation (`HmacJwtTokenService`), refresh-token hashing,
    reactive resource-server conversion, CORS.
  - `event` — Kafka topic constants (`KafkaTopics`) and the platform event records
    (`UserRegisteredEvent`, `OrganizationRegisteredEvent`, `InterviewCreatedEvent`,
    `InterviewStartedEvent`, `InterviewCompletedEvent`, `InterviewScheduledEvent`,
    `ReportGeneratedEvent`, `IdentityEmailEvent`) plus the `EventEnvelope` transport record.
  - `observability` — `CorrelationIdWebFilter` that propagates `X-Request-Id`.
  - `exception`, `validation`, `logging`, `dto`, `common`, `config`, `api-contract` —
    shared error contract, validation assertions, structured logging and API conventions.
- `services`: 19 Spring Boot 4.1 WebFlux services, each with `domain`, `repository`, `service`,
  `web` (+ `web/dto`) and `config` layers, Flyway migrations, per-service PostgreSQL database,
  JWT-secured API and unit tests.
- `infra`: Docker Compose (PostgreSQL, Redis, Kafka, MinIO + the full service stack).
- `docs`: API contract, database design notes and Mermaid diagrams.

## Service Inventory

| Service | Port | Database | Responsibility |
| --- | --- | --- | --- |
| api-gateway | 8080 | — | Routing, JWT auth, Redis rate limiting, CORS, correlation ids |
| discovery-service | 8761 | — | Lightweight in-memory service registry with heartbeat eviction |
| desktop-client-service | 8086 | — | WebSocket session relay bridging desktop clients and Kafka |
| identity-service | 8081 | identity_db | Users, roles, permissions, auth, sessions, refresh tokens |
| organization-service | 8082 | organization_db | Tenant organization, departments, teams, plans, subscriptions |
| recruiter-service | 8083 | recruiter_db | Recruiter profiles, pipeline stages, candidate assignments, notes |
| candidate-service | 8084 | candidate_db | Candidates, profiles, documents, assessments, consents, tags |
| interview-service | 8085 | interview_db | Interviews, sessions, panels, feedback, calendar events; publishes interview lifecycle events |
| telemetry-service | 8087 | telemetry_db | Telemetry events/sessions ingestion; consumes `telemetry.received.v1` |
| policy-engine-service | 8088 | policy_db | Policies, rules, violations; consumes `policy.violation.v1` |
| report-service | 8089 | report_db | Reports, sections, schedules; publishes `report.generated.v1` |
| notification-service | 8090 | notification_db | Notifications, templates, preferences, deliveries |
| analytics-service | 8091 | analytics_db | Analytics queries and job runs |
| audit-service | 8092 | audit_db | Append-only audit events |
| storage-service | 8093 | storage_db | Buckets, objects, versions, signed URLs |
| feature-flag-service | 8094 | feature_flag_db | Features, flags, experiments |
| scheduler-service | 8095 | scheduler_db | Scheduled jobs, executions, distributed job locks |
| integration-service | 8096 | integration_db | Integrations, connections, webhooks, sync logs |
| configuration-service | 8097 | configuration_db | Tenant configuration and schemas |

## Technology

- Java 21, Spring Boot 4.1, WebFlux (reactive), Spring Security 7.1 with `oauth2-jose`.
- Spring Data R2DBC + PostgreSQL; Flyway migrations; row-level security policies per tenant.
- Reactor Kafka for event publishing and consuming; Jackson 3 (`tools.jackson`) serialization.
- Gradle 9.3 Kotlin DSL; quality gates via Spotless, Checkstyle, PMD, SpotBugs and
  Error Prone (`-Werror`), with JaCoCo coverage and springdoc-generated OpenAPI docs.

## Security And Tenancy

Every service is a JWT resource server using the shared `PlatformJwtAuthenticationConverter`;
endpoints are authenticated except OpenAPI/actuator health. The JWT carries `organizationId` and
`userId`, which controllers resolve through `SecurityPrincipals`. Services re-check organization
membership in business logic (defense in depth) in addition to the database RLS policies.

## Event Flow

1. Identity/organization events announce new users and tenants (`identity.user-registered.v1`,
   `organization.registered.v1`); the notification service consumes `identity.email.v1` to render
   and dispatch email, deduplicating on the source `eventId`.
2. The interview service publishes lifecycle events (`interview.created/scheduled/started/
   completed.v1`).
3. Telemetry ingested through WebFlux APIs and desktop relay is persisted and published
   (`telemetry.received.v1`); the policy engine evaluates it and emits `policy.violation.v1`.
   Telemetry consumers commit offsets manually, retry with bounded backoff and route poison
   messages to `telemetry.received.dlq.v1`.
4. The desktop relay broadcasts platform topics to connected clients over WebSocket (`/ws/desktop`);
   the report service publishes `report.generated.v1` for downstream consumption.

## Privacy And Consent

The desktop client collects telemetry only after explicit candidate authorization. Consent state is
tracked in the candidate service (`candidate_consents`) and enforced by the policy engine.

## Building

```bash
JAVA_HOME=/opt/java/jdk-21 ./gradlew build
```

Docker Compose brings up the infrastructure and every service with per-service databases
(`infra/docker/docker-compose.yml`, `infra/docker/init-databases.sh`).
