# 09 — Build the Docker Images

**Purpose.** To build the 19 service Docker images that will run in EKS. The build is done once
per commit; the same images are then pushed to ECR and deployed to every environment.

## Prerequisites

- Steps 01–08 completed.
- Docker installed and the daemon running (`docker info`).
- Java 21 + Gradle available (the images build from jars; see `development/README.md`).

## Estimated Time

20–40 minutes on first run (Gradle downloads), 5–10 minutes cached.

## Required AWS permissions

None — building images happens locally. (Pushing them is step 10.)

## What a service image contains

Each `services/<service>/Dockerfile` follows the same pattern:

1. **Stage 1 (build)** — a Gradle/JDK 21 image compiles the service to a jar.
2. **Stage 2 (runtime)** — a minimal JRE 21 base runs `java -jar <service>-0.1.0.jar`.

The runtime image is intentionally slim (no build tools, no source) and runs as a non-root user.
This is what makes the images safe to run in production.

## Build strategy

The platform ships 19 services. The canonical, reproducible build is **Gradle's** — the workflow
`deploy.yml` builds jars with `./gradlew bootJar` and Docker builds the images. For a local build
you can let Gradle produce all jars first, then build images per service:

### Step 1 — Build all jars (fast incremental cache)

```bash
./gradlew clean bootJar -x test
```

**What this does:** compiles every service and produces
`services/<name>/build/libs/<name>-0.1.0.jar`. `-x test` skips tests for speed (run tests
separately with `./gradlew check`).

### Step 2 — Build one image (example: identity-service)

```bash
docker build -t integrity-dev/identity-service:dev \
  -f services/identity-service/Dockerfile services/identity-service
```

**What this does:** runs the two-stage Dockerfile and tags the result
`integrity-dev/identity-service:dev`.

### Step 3 — Build all images

```bash
for svc in api-gateway discovery-service identity-service organization-service \
           recruiter-service candidate-service interview-service desktop-client-service \
           telemetry-service policy-engine-service report-service notification-service \
           analytics-service audit-service storage-service feature-flag-service \
           scheduler-service integration-service configuration-service; do
  docker build -t "integrity-dev/${svc}:dev" \
    -f "services/${svc}/Dockerfile" "services/${svc}"
done
```

> **Why the `integrity-<env>/` prefix in the tag?** It matches the ECR repository naming
> (`integrity-<env>/<service>`) so the push step is a straight copy, and it prevents accidental
> cross-environment confusion. For production use the real ECR URI (step 10).

## Expected output

```text
=> [build 1/3] ...  (Gradle compile)
=> [runtime 2/2] ...
DONE ... tagged integrity-dev/identity-service:dev
```

## Verification steps

```bash
# The image exists locally
docker images | grep integrity-dev

# The image actually starts (identity-service example)
docker run --rm -d --name smoke-id \
  -e SPRING_PROFILES_ACTIVE=local \
  -p 8081:8081 \
  integrity-dev/identity-service:dev
sleep 20
curl -sf http://localhost:8081/actuator/health | grep '"UP"' && echo OK
docker stop smoke-id
```

> The smoke run needs the local Postgres/Redis (see `development/README.md` §4) for a full
> `"UP"`; with infra up it should report UP.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `docker: command not found` | Docker not installed/daemon stopped | Start Docker Desktop; `docker info` |
| `Cannot connect to the Docker daemon` | Daemon down or socket permission | Start the daemon; add your user to the `docker` group |
| `Failed to resolve: openjdk:21-...` | No network / registry block | Check connectivity to `docker.io` |
| `no matching manifest for linux/arm64` | Architecture mismatch in a pinned base | Use the same platform as the cluster (`--platform linux/amd64`) or build on amd64 |
| Gradle build fails on one module | A test/compile issue in that service | Fix the module, then re-run only it: `./gradlew :services:<svc>:bootJar` |

## Rollback procedure

- Building images is non-destructive. To remove test images:
  ```bash
  docker image rm integrity-dev/identity-service:dev
  ```
- The source of truth is the Dockerfile + jar; anything misbuilt can be rebuilt from the same
  commit.

## Best practices

- **Tag with the commit SHA for anything you intend to deploy**: `:9cbbf32`, not `:dev` or
  `:latest`. Immutable SHA tags are what make rollbacks possible (`deployment.md` §3).
- Never build images on a production machine; build in CI (`deploy.yml`).
- Scan images for vulnerabilities before pushing (ECR does this on push too).

## Security notes

- Run images as non-root (the Dockerfiles already do) — enforced in Kubernetes via
  `runAsNonRoot`.
- Use a minimal JRE base; smaller images have a smaller attack surface.
- Do not bake secrets or `.env` into images. All configuration is injected at runtime
  (ConfigMap/Secret).
