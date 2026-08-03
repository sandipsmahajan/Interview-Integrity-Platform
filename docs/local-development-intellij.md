# Local Development in IntelliJ IDEA

This guide explains how to open, build, and run all 19 services of the platform in IntelliJ IDEA on
your machine for a local integration test. Everything runs on `localhost`, so no external
infrastructure is needed beyond Docker for PostgreSQL, Kafka and Redis.

## Prerequisites

- JDK 21 (Temurin/OpenJDK 21+). The Gradle build and all services target Java 21.
- Docker with Docker Compose (for the infrastructure containers).
- IntelliJ IDEA 2024.1 or newer (Gradle 9.3 support; older versions may not parse the Kotlin DSL).
- At least 8 GB of free memory; the full stack consumes roughly 4-6 GB when all services run.

## 1. Open the project in IntelliJ

1. In IntelliJ choose `File > Open` and select the repository root (`/workspace`, the folder that
   contains `settings.gradle.kts` and `build.gradle.kts`).
2. When prompted to "Trust Project", select **Trust Project**.
3. IntelliJ imports the Gradle project automatically. Wait for the sync to finish (bottom-right
   progress bar). If sync is not automatic, open the Gradle tool window and click the refresh icon.

## 2. Configure the JDK and Gradle JVM

1. `File > Project Structure > Project`:
   - `SDK`: select a JDK 21 (or add one via `Add SDK > Download JDK` and pick version 21).
   - `Language level`: 21.
2. `Settings > Build, Execution, Deployment > Build Tools > Gradle`:
   - `Gradle JVM`: select the same JDK 21.
   - `Build and run using`: Gradle.
3. `Settings > Build, Execution, Deployment > Build Tools > Gradle > Toolchain Detection`: make
   sure a Java 21 toolchain is detected.

## 3. Build the project

The root `gradle.properties` enables the Gradle configuration cache. From IntelliJ's built-in
terminal:

```bash
./gradlew build
```

Alternatives:

- IntelliJ menu `Build > Build Project` uses IntelliJ's own compiler and is unaffected by the
  configuration cache; it is enough for running services from run configurations.
- In the Gradle tool window, add a run configuration for `build`.

Expected result: `BUILD SUCCESSFUL` after compiling the 10 shared libraries and 19 services.

## 4. Start the infrastructure

Only the infrastructure containers are needed locally (PostgreSQL, Kafka, Redis, MinIO). The
service containers in the compose file are not started in this flow because the services run from
IntelliJ instead.

```bash
docker compose -f infra/docker/docker-compose.yml up -d postgres redis kafka minio
```

On the first boot, `infra/docker/init-databases.sh` creates the 16 per-service databases
(`identity_db`, `organization_db`, ... `configuration_db`). Verify they exist:

```bash
docker exec -it interview-integrity-postgres-1 psql -U integrity -c "\l"
```

If the script does not run (for example the volume already existed), create the databases
manually by running the `CREATE DATABASE` statements from `infra/docker/init-databases.sh` inside
the container.

MinIO is optional: `storage-service` issues signed URLs itself and does not require an object store
to start.

## 5. Run the services

Open `Edit Configurations...` (`Run > Edit Configurations`), press `+ > Application`, and set:

- `Main class`: the fully qualified name of the service application (see the table below).
- `Use classpath of module`: the matching `<service>` Gradle module.
- `Working directory`: leave at the project root.
- Optionally set the environment variable `JWT_SECRET` (must be the same across all services and at
  least 32 bytes). When unset, the shared default from `application.yml` is used automatically.

Create one such run configuration per service you want to start. Suggested start order:

1. **identity-service** — bootstrap your tenant (see smoke test below) and issue JWTs.
2. Any domain service you want to exercise (organization, recruiter, candidate, interview, ...).
3. **api-gateway** (optional) to reach every service through port 8080.
4. **discovery-service** and **desktop-client-service** only if you exercise the registry or the
   WebSocket relay.

The gateway routes to `http://localhost:<port>`, so the target services must run on their standard
ports (their defaults are already configured).

## Service ports and main classes

| Service | Port | Main class |
| --- | --- | --- |
| api-gateway | 8080 | `com.interviewintegrity.gateway.GatewayApplication` |
| identity-service | 8081 | `com.interviewintegrity.identity.IdentityServiceApplication` |
| organization-service | 8082 | `com.interviewintegrity.organization.OrganizationServiceApplication` |
| recruiter-service | 8083 | `com.interviewintegrity.recruiter.RecruiterServiceApplication` |
| candidate-service | 8084 | `com.interviewintegrity.candidate.CandidateServiceApplication` |
| interview-service | 8085 | `com.interviewintegrity.interview.InterviewServiceApplication` |
| desktop-client-service | 8086 | `com.interviewintegrity.desktopclient.DesktopClientServiceApplication` |
| telemetry-service | 8087 | `com.interviewintegrity.telemetry.TelemetryServiceApplication` |
| policy-engine-service | 8088 | `com.interviewintegrity.policy.PolicyEngineServiceApplication` |
| report-service | 8089 | `com.interviewintegrity.report.ReportServiceApplication` |
| notification-service | 8090 | `com.interviewintegrity.notification.NotificationServiceApplication` |
| analytics-service | 8091 | `com.interviewintegrity.analytics.AnalyticsServiceApplication` |
| audit-service | 8092 | `com.interviewintegrity.audit.AuditServiceApplication` |
| storage-service | 8093 | `com.interviewintegrity.storage.StorageServiceApplication` |
| feature-flag-service | 8094 | `com.interviewintegrity.featureflag.FeatureFlagServiceApplication` |
| scheduler-service | 8095 | `com.interviewintegrity.scheduler.SchedulerServiceApplication` |
| integration-service | 8096 | `com.interviewintegrity.integration.IntegrationServiceApplication` |
| configuration-service | 8097 | `com.interviewintegrity.configuration.ConfigurationServiceApplication` |
| discovery-service | 8761 | `com.interviewintegrity.discovery.DiscoveryServiceApplication` |

Start each with the green Run button. A service is healthy when its log shows the port it is
listening on and Flyway finished its migrations.

## 6. Smoke test

1. Confirm health on any service:

```bash
curl http://localhost:8081/actuator/health
```

2. Bootstrap a tenant and its first administrator (identity must be running):

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"companyName":"Acme Inc","adminEmail":"admin@acme.test","adminPassword":"Passw0rd!","adminDisplayName":"Admin"}'
```

The response contains `accessToken` and `refreshToken`.

3. Login to obtain a fresh token:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@acme.test","password":"Passw0rd!"}'
```

4. Call a protected endpoint of any running service using the returned token:

```bash
curl http://localhost:8082/api/v1/organizations \
  -H "Authorization: Bearer <accessToken>"
```

5. Through the gateway (when api-gateway is running):

```bash
curl http://localhost:8080/api/v1/organizations \
  -H "Authorization: Bearer <accessToken>"
```

6. Browse OpenAPI documentation on any service:
   `http://localhost:<port>/swagger-ui.html`.

## 7. Run the tests

From the Gradle tool window select the module and run `check`, or from the terminal:

```bash
./gradlew :services:<service>:check
```

Run the whole suite:

```bash
./gradlew check
```

## Troubleshooting

- **`Timeout waiting to lock` / `Configuration cache` errors**: retry the Gradle invocation, or run
  the services via Application run configurations instead of Gradle tasks.
- **`FlywayException: ... database does not exist`**: the per-service database is missing. Ensure
  the init script ran, or create the database manually (see step 4).
- **Connection refused on `localhost:5432` / `localhost:9092`**: the infrastructure containers are
  not running. Start them with Docker Compose (step 4).
- **Kafka-dependent services log "Unable to connect to Kafka"**: telemetry, policy-engine,
  desktop-client and the publishers retry; start Kafka first and restart the service if needed.
- **`401 Unauthorized` on a protected call**: the token was issued by a service started with a
  different `JWT_SECRET` than the one validating it. Use one shared `JWT_SECRET`.
- **Port already in use**: another instance is running; stop it or change `server.port` in the
  service's `application.yml`.
- **Cross-tenant `404`**: services guard resources by the `organizationId` embedded in the JWT, so
  a token from tenant A cannot read tenant B resources even if the resource id exists.
