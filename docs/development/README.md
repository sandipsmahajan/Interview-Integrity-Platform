# Local Development Guide

**Purpose.** To take a brand-new developer from an empty machine to a fully running Integrity Pro
stack on their laptop: infrastructure containers, all 19 services, the recruiter portal, plus
debugging, stopping, and cleaning up.

> **Time estimate**: 30–60 minutes the first time (mostly downloads). Subsequent cold starts take
> 5–10 minutes.

---

## 1. Prerequisites

Install **all** of these before continuing. Versions are pinned to what the repository expects.

### 1.1 Java 21 (JDK)

The backend is Java 21, Spring Boot 4.1. Gradle can auto-provision a JDK into `~/.gradle/jdks`,
but an explicit JDK 21 is faster.

```bash
# macOS (Homebrew)
brew install --cask temurin@21

# Ubuntu / Debian
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk

# Verify
java -version   # must print "openjdk ... 21.x"
```

**Why Java 21?** The Gradle toolchain (`build.gradle.kts`) declares
`JavaLanguageVersion.of(21)`; every service compiles against it.

### 1.2 Node.js 24 and npm 11

The portals (`portals/recruiter`, `portals/admin`) are React + Vite.

```bash
# Use nvm to install the exact range
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
nvm install 24
nvm use 24

# Verify
node --version   # 24.x
npm --version    # 11.x
```

The portal `package.json` declares `"node": ">=24 <25"` and `"npm": ">=11"`. A wrong Node
version will fail the install with an `EBADENGINE` warning and may break the Vite build.

### 1.3 Rust (for the desktop client, optional)

The desktop client (`client/`) is a Rust workspace (edition 2021). You only need Rust if you are
developing the client.

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
rustup toolchain install stable
cargo --version   # verify
```

### 1.4 Docker Desktop

The platform's data plane (Postgres, Redis, Kafka, MinIO, Mailpit) runs in Docker.

- **macOS / Windows**: install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  and start it. Give it at least 4 GB RAM and 2 CPUs (Preferences → Resources).
- **Linux**: install Docker Engine + the `docker compose` plugin:
  ```bash
  sudo apt-get install -y docker-ce docker-ce-cli docker-compose-plugin
  ```

Verify with:

```bash
docker --version
docker compose version
```

**Why Docker?** `scripts/run-services.sh` uses `docker compose` to bring up the infrastructure
before any service starts. Without Docker the backend has no database, cache, or broker.

### 1.5 Terraform (only for infra work)

```bash
# Install via the official tool (see deployment/04-install-terraform.md for full steps)
brew install terraform        # macOS
sudo apt-get install -y terraform  # Debian/Ubuntu (HashiCorp repo)
terraform version   # 1.9.x (matches terraform/versions.tf)
```

### 1.6 AWS CLI (only for AWS work)

```bash
# macOS
brew install awscli
# or the official installer, see deployment/03-configure-aws-cli.md
aws --version   # 2.x
```

### 1.7 kubectl and Helm (only for Kubernetes work)

```bash
brew install kubectl helm      # macOS
# See deployment/05-install-kubectl.md and 06-install-helm.md for full steps
kubectl version --client
helm version
```

### 1.8 Git

```bash
git --version   # 2.40+
```

### 1.9 IntelliJ IDEA (recommended) and VS Code

- **IntelliJ IDEA Ultimate** — open the repo root as a Gradle project. It auto-imports
  `settings.gradle.kts` and all 19 modules. Enable the **Spring Boot** and **Gradle** plugins.
  Set the Gradle JVM to JDK 21 (Settings → Build Tools → Gradle → Gradle JVM). See
  `local-development-intellij.md` for the full walkthrough.
- **VS Code** — install the **Java Extension Pack**, **Spring Boot Extension Pack**, and the
  **Rust Analyzer** (for `client/`). Run `./gradlew build` once so the IDE can index classes.

---

## 2. Clone the repository

```bash
git clone https://github.com/sandipsmahajan/Interview-Integrity-Platform.git
cd Interview-Integrity-Platform
```

If you have already cloned it:

```bash
git checkout master
git pull --rebase
```

**Why `master`?** All releases are cut from `master`; feature work happens on branches (see
`best-practices.md`).

---

## 3. Configure your environment

The platform reads environment variables and overlay config files.

### 3.1 Copy the example environment file

```bash
cp infra/config/.env.example .env
```

**What is this file?** `.env` is read by Docker Compose and by the local scripts. It documents
every configuration key the platform understands (`DB_HOST`, `KAFKA_BOOTSTRAP_SERVERS`,
`REDIS_URL`, `MINIO_*`, etc.). Do **not** commit it — it is `.gitignore`'d.

### 3.2 Choose the `local` profile

Default `SPRING_PROFILES_ACTIVE=local`. The overlay file
`infra/config/application-local.yml` is merged over each service's own `application.yml`.

**What the profile controls:**

| Setting | Local value | Why |
|---|---|---|
| `DB_HOST` | `localhost` | Postgres is reachable on the host via compose port mapping |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Local Kafka via compose |
| `spring.data.redis.*` | `localhost:6379` | Local Redis via compose |
| `platform.storage.endpoint` | `http://localhost:9000` | Local MinIO |
| `spring.mail.*` | Mailpit `localhost:8025` | Catch-all inbox in the browser |

**Verify your `.env`**:

```bash
grep -E 'SPRING_PROFILES_ACTIVE|DB_HOST|KAFKA' .env
```

You should see `SPRING_PROFILES_ACTIVE=local` and `DB_HOST=localhost`.

---

## 4. Start the infrastructure

All infrastructure runs in Docker Compose. The script does this for you, but understanding the
manual step matters for debugging.

```bash
docker compose -f infra/docker/docker-compose.yml up -d postgres redis kafka minio mailpit
```

**What each container is** (explained):

| Container | What it is | Why it must be running |
|---|---|---|
| `postgres` | PostgreSQL 16, creates the 16 databases on first boot | The services' single source of truth |
| `redis` | Redis cache | Sessions/refresh tokens, hot-read caching |
| `kafka` | Kafka broker (Strimzi-compatible or KRaft) | Event backbone for telemetry + domain events |
| `minio` | S3-compatible object storage | Documents, reports, uploads |
| `mailpit` | Local SMTP server + web UI | Email without a real inbox |

Verify all five are healthy:

```bash
docker compose -f infra/docker/docker-compose.yml ps
```

All should show `Up` and a healthy status. Wait ~15 seconds for Kafka to finish booting; it takes
longest to become usable.

**Verify each endpoint**:

```bash
# PostgreSQL accepts connections
docker compose -f infra/docker/docker-compose.yml exec postgres pg_isready -U integrity

# MinIO console (credentials from infra/config/docker-compose.yml)
open http://localhost:9001   # MinIO console
open http://localhost:8025   # Mailpit inbox
```

---

## 5. Run Flyway migrations

Flyway runs automatically when each service starts. **You do not need a separate migration step.**
If you want to migrate a single database explicitly (for example while developing one service):

```bash
# Run only the migrations of the identity service
./gradlew :services:identity-service:bootRun &
```

When the service reaches "Started identity-service", Flyway has applied
`services/identity-service/src/main/resources/db/migration/*.sql`. Check the per-database
`flyway_schema_history` table to confirm:

```bash
docker compose -f infra/docker/docker-compose.yml exec postgres \
  psql -U integrity -d identity_db -c "SELECT version, description, success FROM flyway_schema_history;"
```

**Why "success = t"?** Flyway marks each migration `success` only if it committed. Any `f`
indicates a failed migration — see `troubleshooting/README.md` → "Flyway failures".

---

## 6. Run every service

The one-command way (builds missing jars, then starts all 19 in the correct order):

```bash
scripts/run-services.sh
```

**What this script does, step by step:**

1. Starts `postgres redis kafka minio` via Docker Compose (unless `--no-infra`).
2. Builds missing `bootJar` artifacts with Gradle (unless `--no-build`).
3. Starts `discovery-service:8761`, waits for `/actuator/health` to report `UP`.
4. Starts `api-gateway:8080`, waits for health.
5. Starts all remaining services in parallel (ports 8081–8097), health-checking each.
6. Prints the summary banner when the whole stack is up.

**Useful flags** (from the script header):

```bash
scripts/run-services.sh --no-infra   # reuse already-running infra containers
scripts/run-services.sh --no-build   # reuse already-built jars (fast restart)
scripts/run-services.sh stop         # stop all started services (kills by PID file)
scripts/run-services.sh logs         # tail all service logs (Ctrl-C to exit)
```

**Why this ordering?** Services register with the discovery service at startup and the gateway
needs the registry to route. Starting the registry and gateway first avoids "service not found"
errors and allows each service to register cleanly as it boots.

> **Tip**: The first run builds all 19 jars and downloads Gradle + dependencies — it can take
> 10–20 minutes. Subsequent `--no-build` starts take ~2 minutes.

---

## 7. Run the recruiter portal

Open a second terminal:

```bash
# Install portal dependencies once
npm --prefix portals install

# Start the Vite dev server
npm --prefix portals/recruiter run dev
```

or use the helper script (which also makes sure the portal's backend services are up):

```bash
scripts/run-recruiter-portal.sh
```

The portal runs at:

```
http://localhost:5173
```

**How the portal reaches the backend:** `portals/recruiter/vite.config.ts` proxies `/api` to
`http://localhost:8080` (the gateway). You never call services directly from the browser.

**Admin portal** (if needed):

```bash
npm --prefix portals/admin run dev   # serves on its own Vite port
```

---

## 8. Verify the installation

Run every check below; each failure maps to a section in `troubleshooting/README.md`.

### 8.1 Infrastructure

```bash
docker compose -f infra/docker/docker-compose.yml ps   # all "Up"
```

### 8.2 Discovery (registry)

```bash
open http://localhost:8761
```

The Eureka dashboard should list **19 registered applications** (DISCOVERY-SERVICE,
API-GATEWAY, IDENTITY-SERVICE, …). This is the single best "is everything registered" check.

### 8.3 Gateway

```bash
curl -s http://localhost:8080/actuator/health | jq
# {"status":"UP"}
```

### 8.4 Every service

```bash
for port in 8761 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 \
            8091 8092 8093 8094 8095 8096 8097; do
  printf '%s ' "$port"
  curl -sf "http://localhost:$port/actuator/health" | grep -o '"status":"[A-Z]*"'
done
```

Every service must report `"UP"`.

### 8.5 Smoke test through the gateway

```bash
# Health through the gateway (proves routing works)
curl -s http://localhost:8080/api/v1/health

# A real login (adjust payload to the identity contract in docs/api.md)
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@example.com","password":"ChangeMe123!"}'
```

A JSON body with an `accessToken`/`refreshToken` proves the full chain
(portal→gateway→identity→DB→Redis).

### 8.6 Portals

- `http://localhost:5173` loads the recruiter portal without console errors.
- Log in with a seeded account (see identity-service seed data) and create a candidate.

---

## 9. Debugging

### 9.1 Service logs

```bash
# Tail all service logs (Ctrl-C to exit)
scripts/run-services.sh logs

# One service
tail -f build/logs/identity-service.log

# Search for errors
grep -iE 'error|exception' build/logs/*.log
```

### 9.2 Run one service in the IDE (IntelliJ)

1. Choose the service's `main` class (e.g. `IdentityServiceApplication`).
2. Set environment variable `SPRING_PROFILES_ACTIVE=local`.
3. Run/Debug. The debugger attaches on port 5005 if you use the `bootRun` JVM args in the README
   run configuration.
4. Breakpoints work across service boundaries when the gateway and the target service both run
   in debug mode.

### 9.3 Run one service from the CLI with debug

```bash
./gradlew :services:identity-service:bootRun --args="--debug.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### 9.4 Inspect databases

```bash
docker compose -f infra/docker/docker-compose.yml exec postgres \
  psql -U integrity -d identity_db
```

### 9.5 Inspect Kafka

```bash
docker compose -f infra/docker/docker-compose.yml exec kafka \
  kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 9.6 Inspect Redis

```bash
docker compose -f infra/docker/docker-compose.yml exec redis redis-cli INFO keyspace
```

---

## 10. Stopping services

```bash
# Stop the Java services started by the scripts (graceful, kills by PID file)
scripts/run-services.sh stop

# Stop infrastructure containers
docker compose -f infra/docker/docker-compose.yml down
```

**Order matters:** stop the Java services first (they flush/close cleanly), then the containers.

---

## 11. Cleaning your environment

Remove everything (containers + volumes + build artifacts). **This destroys local data** — the
16 databases, MinIO buckets, and Kafka topics are wiped.

```bash
# Containers AND volumes (volumes hold the databases)
docker compose -f infra/docker/docker-compose.yml down -v

# Remove all built jars and logs
./gradlew clean
rm -rf build/           # script logs + pid files
rm -rf build/logs build/pids

# Optional: node_modules and Rust target dirs
rm -rf portals/node_modules portals/*/node_modules
rm -rf client/target

# Optional: git-ignored Terraform caches
find terraform -name .terraform -type d -prune -exec rm -rf {} \; 2>/dev/null || true
```

> The `-v` flag in `docker compose down -v` is what deletes the Postgres data volume. If you want
> to **keep** your local data, run `docker compose down` without `-v`.

---

## 12. Common local problems (quick map)

| Symptom | Cause | Fix |
|---|---|---|
| `Connection refused: localhost:5432` | Postgres not up | `docker compose -f infra/docker/docker-compose.yml up -d postgres` |
| `KafkaError: Local: Broker transport failure` | Kafka still booting | Wait 15 s, retry; check `docker compose ps kafka` |
| Service log shows `FlywayException: Validate failed` | Migration edited after apply | See "Flyway failures" in troubleshooting |
| `EADDRINUSE :8080` | Another gateway instance | `scripts/run-services.sh stop`, kill leftover PID, restart |
| Portal proxy 502 | Gateway not up | Wait for `http://localhost:8080/actuator/health` to be `UP` |
| Gradle toolchain download slow | First build | Be patient; it caches in `~/.gradle` |
| Node `EBADENGINE` | Wrong Node version | `nvm use 24` |
