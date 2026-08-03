# Architectural Design Decisions

**Purpose.** An ADR-style log of every significant design decision, why it was made, and what the
alternatives were. When you disagree with a decision, read its ADR first.

## D1. Microservices with database-per-service

- **Decision**: Split the platform into 19 Java 21 / Spring Boot reactive microservices, each with
  its own PostgreSQL database.
- **Rationale**: Independent schema evolution, independent scaling of the telemetry path, bounded
  blast radius, and small deployable units that match the team's stream-aligned structure.
- **Alternatives rejected**: Monolith (too coupled for burst telemetry + report pipelines); shared
  database (schema coupling, no ownership).
- **Cost**: More moving parts (registry, tracing, distributed transactions). Mitigated by the
  gateway + event backbone and the shared libraries.

## D2. Reactive (WebFlux) everywhere

- **Decision**: All services use Spring WebFlux (reactive).
- **Rationale**: Telemetry ingestion is a burst-heavy I/O workload; non-blocking servers hold
  dramatically more concurrent connections per node.
- **Alternative**: Spring MVC (blocking) — simpler but throughput-bound under telemetry bursts.

## D3. API gateway as the only public entry point

- **Decision**: All external traffic enters through `api-gateway` (`:8080`); no service is
  publicly exposed.
- **Rationale**: One place to enforce auth, CORS, rate limiting and TLS behavior; one attack
  surface to harden; services can be firewalled entirely from the internet.
- **Trade-off**: gateway is a single hop — mitigated by horizontal scaling and readiness gating.

## D4. Eureka-style discovery service

- **Decision**: `discovery-service` (`:8761`) is a Netflix-Eureka-compatible registry; all
  services register and resolve by logical name.
- **Rationale**: Works identically in Docker Compose, Kubernetes, and bare metal, so local
  development matches production behavior without a cloud-specific registry.
- **Alternative**: Kubernetes-native DNS / headless services only — weaker for compose parity.

## D5. Configuration-only environment differences

- **Decision**: The same codebase runs in `local/dev/qa/uat/prod`; environments differ only via
  `SPRING_PROFILES_ACTIVE` and environment variables using the `${ENV_VAR:default}` pattern.
- **Rationale**: Promotion becomes a config diff, not a code diff; "prod runs what uat ran" is
  provable. No business-logic branches per environment.
- **Rule enforced**: No environment-specific Java code; profile files in `infra/config/` and
  per-env Helm `values-*.yaml`.

## D6. Kafka as the integration backbone

- **Decision**: Domain events and telemetry flow through Kafka; producers never wait on consumers.
- **Rationale**: Telemetry bursts are decoupled from policy/report processing; consumers can
  replay; audit becomes an immutable event stream.
- **Trade-off**: Eventual consistency between producers and consumers — acceptable for integrity
  reporting, which is fundamentally asynchronous.

## D7. Same topology on Strimzi and Amazon MSK

- **Decision**: Local/dev run Strimzi Kafka; qa/uat/prod run Amazon MSK. Applications only see
  bootstrap-address + auth configuration.
- **Rationale**: Free local dev with zero cloud cost, managed brokers in production; the
  migration path is configuration-only (see `kafka-msk-migration.md`).

## D8. Per-environment Terraform roots over workspaces

- **Decision**: Each environment is a separate Terraform root directory with its own state
  backend.
- **Rationale**: Environments are deliberately different in size and feature (WAF, multi-AZ,
  rotation), and isolation of state prevents a misapplied `prod` change from touching `dev`.
  Blast radius per state file is one environment.
- **Trade-off**: Some duplication between roots — kept minimal because the roots are thin
  compositions over shared modules.

## D9. Remote state with locking

- **Decision**: State lives in S3 (per env) with a DynamoDB lock table; never local.
- **Rationale**: Collaborative safety: concurrent applies are blocked by the lock; state survives
  laptop loss; encryption via KMS.

## D10. One umbrella Helm chart

- **Decision**: All 19 services ship from one chart (`infra/helm/interview-integrity`) with
  per-env values files.
- **Rationale**: One `helm upgrade` per release → no cross-chart version skew; consistent
  templates (probes, HPA, PDB, network policy) for every service.

## D11. Pipeline-owned config/secret materialization

- **Decision**: The deploy pipeline renders `integrity-config` (ConfigMap) from `infra/config/`
  and `integrity-secrets` (Secret) from GitHub secrets/Secrets Manager; the chart only references
  them.
- **Rationale**: Secrets never enter Helm values; config diffs are reviewable in Git; a config
  change triggers a rollout via the checksum annotation.

## D12. Immutable, traceable images

- **Decision**: Images are built once per commit, tagged with the short SHA, never overwritten.
- **Rationale**: Rollback = "deploy the last known-good tag"; images are reproducible from Git.

## D13. ALB → ingress-nginx → services (no service mesh by default)

- **Decision**: The public path is ALB → ingress-nginx (NodePort 30080) → gateway/services.
- **Rationale**: Simplest production-grade front door with AWS-managed TLS and WAF support;
  mTLS remains optional (documented) rather than mandatory complexity.

## D14. Default-deny network policies

- **Decision**: The `integrity` namespace is default-deny on ingress and egress with explicit
  allow rules.
- **Rationale**: Kubernetes is wide-open by default; explicit allows bound compromised pods and
  satisfy compliance expectations.

## D15. AWS-managed data plane in prod

- **Decision**: RDS, MSK, ElastiCache, S3, SES in qa/uat/prod; in-cluster equivalents in local/dev.
- **Rationale**: Managed backups, PITR, multi-AZ, patching, and monitoring for production; cheap
  and fast for development.

## D16. Secrets Manager as source of truth for secrets

- **Decision**: All secrets (JWT, DB, SCRAM) live in AWS Secrets Manager with KMS encryption and
  rotation for prod.
- **Rationale**: Central, auditable, rotatable; CI/pods fetch at runtime or deploy time via IRSA.

## D17. GitHub Actions with OIDC

- **Decision**: CI/CD runs on GitHub Actions; AWS access via OIDC federation, never static keys.
- **Rationale**: Zero standing credentials; per-environment roles; approval gates via GitHub
  environments.

## D18. Self-hosted dev data plane in cluster

- **Decision**: `dev` runs Postgres/Redis/Kafka(Strimzi)/MinIO/Mailpit inside the cluster.
- **Rationale**: Real Kubernetes behavior at near-zero cost; the same Helm/k8s manifests as prod
  minus the AWS-managed data plane.

## How to propose a new decision

1. Create a short ADR with **Context → Decision → Consequences**.
2. Reference the runbook/architecture doc it affects.
3. Get review; record the date and alternative considered.
4. Update the relevant architecture page so docs and reality stay in sync.
