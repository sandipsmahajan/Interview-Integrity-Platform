# Integrity Pro — Documentation

This is the complete operations and deployment documentation set for the **Integrity Pro**
platform (also referred to as the **Interview Integrity Platform**). It is written for engineers
who are deploying, operating, monitoring, maintaining, upgrading, or recovering the platform for
the first time, with little or no AWS experience.

The docs assume a **brand-new AWS account** and take you step by step to a **production-ready
Amazon EKS environment** using nothing more than this documentation, Terraform, Helm, kubectl, and
the GitHub Actions pipelines already present in the repository.

---

## 1. How the documentation is organised

| Directory | Audience | What it covers |
|---|---|---|
| [`architecture/`](architecture/README.md) | Everyone | How the platform is designed: high-level, microservices, infrastructure, networking, deployment, sequence, database, Kafka, monitoring, and security architecture plus every design decision. |
| [`deployment/`](deployment/README.md) | DevOps, SREs | The 20-step path from a brand-new AWS account to a live EKS deployment (`01` to `20`). |
| [`development/`](development/README.md) | Developers | Local development: prerequisites, running the full stack, portals, debugging, stopping and cleaning up. |
| [`terraform/`](terraform.md) | DevOps, SREs | The Terraform structure: modules, remote state, locking, variables, outputs, environments, plan/apply/destroy/import/upgrade. |
| [`kubernetes/`](kubernetes.md) | DevOps, SREs | Kubernetes concepts as applied here: namespaces, workloads, services, ingress, ConfigMaps, secrets, PVs/PVCs, Helm, HPA, rolling updates. |
| [`monitoring/`](monitoring.md) | SREs, QA | Prometheus, Grafana, CloudWatch, OpenTelemetry, Jaeger, metrics, dashboards, alerts, logs and tracing. |
| [`operations/`](operations/README.md) | Ops, SREs | The operations manual and the indexed list of runbooks. |
| [`runbooks/`](runbooks/README.md) | Ops, SREs | Step-by-step incident runbooks: restarts, crashes, database/Kafka/Redis failures, scaling, rotation, backup, restore, upgrade, rollback. |
| [`security/`](security.md) | Everyone | IAM, secrets, certificates, TLS, network policies, security groups, RBAC, least privilege, encryption. |
| [`disaster-recovery/`](disaster-recovery.md) | Ops, SREs | RDS backups, point-in-time recovery, S3 versioning, restore, DR plan, RTO and RPO. |
| [`troubleshooting/`](troubleshooting/README.md) | Everyone | A-Z troubleshooting guides for Terraform, Helm, Kubernetes, Flyway, databases, Kafka, Redis, ingress, DNS, TLS, and authentication. |
| [`best-practices/`](best-practices.md) | Everyone | Coding, deployment, Git, branch, release, versioning, infrastructure, and promotion standards. |

Supplementary documents in this directory (from earlier work):

- [`api.md`](api.md) — API notes.
- [`architecture.md`](architecture.md) — original architecture overview.
- [`configuration-reference.md`](configuration-reference.md) — every configuration property across all 7 Spring profiles.
- [`database-strategy.md`](database-strategy.md) — multi-database design rationale.
- [`kafka-msk-migration.md`](kafka-msk-migration.md) — Strimzi-to-MSK migration path.
- [`object-storage.md`](object-storage.md) — MinIO/S3 object storage strategy.
- [`backup-disaster-recovery.md`](backup-disaster-recovery.md) — original backup/DR notes.
- [`infrastructure-architecture.md`](infrastructure-architecture.md) — original infrastructure overview.
- [`recruiter-portal-development.md`](recruiter-portal-development.md) — portal development notes.
- [`local-development-intellij.md`](local-development-intellij.md) — IntelliJ setup.
- [`database/`](database/) and [`diagrams/`](diagrams/) — schema notes and Mermaid diagrams.

---

## 2. Suggested reading order

| If you are… | Read this first |
|---|---|
| Deploying to AWS for the first time | `deployment/README.md`, then `deployment/01-*.md` → `deployment/20-*.md` |
| A new developer joining the team | `development/README.md`, then `architecture/README.md` |
| On-call / SRE | `operations/README.md`, `runbooks/README.md`, `troubleshooting/README.md` |
| Reviewing security posture | `security.md` |
| Planning backup / recovery | `disaster-recovery.md` |
| Working on infrastructure | `terraform.md`, `kubernetes.md`, `deployment/07-*.md` → `08-*.md` |

---

## 3. The platform at a glance

- **Backend**: 19 Java 21 / Spring Boot 4.1 reactive microservices (API gateway + 18 domain/data services) built with Gradle (Kotlin DSL). All runtimes are switched through `SPRING_PROFILES_ACTIVE` plus environment variables using the `${ENV_VAR:default}` pattern — no code changes are needed between environments.
- **Desktop client**: Rust workspace (`client/`) — launcher, agent, browser, policy, IPC, network, updater, storage, telemetry, security, screenshare, camera, microphone, system, logger and plugins.
- **Portals**: React + Vite applications (`portals/recruiter`, `portals/admin`).
- **Data plane (local/dev)**: PostgreSQL, Redis, Kafka (Strimzi), MinIO, Mailpit running in Docker/Kubernetes.
- **Data plane (qa/uat/prod)**: Amazon RDS for PostgreSQL, Amazon ElastiCache for Redis, Amazon MSK, Amazon S3, Amazon SES.
- **Infrastructure**: Terraform multi-environment roots (`terraform/environments/{local,dev,qa,uat,prod}`) backed by a shared S3 state bucket with DynamoDB locking.
- **Delivery**: GitHub Actions — CI (`ci.yml`), Terraform plan/apply (`terraform.yml`), build/ECR/Helm deploy (`deploy.yml`).

---

## 4. Conventions used in this documentation

1. **Commands** are shown in code blocks prefixed with `$`. Copy them verbatim; do not type the `$`.
2. **Placeholders** look like `<your-value>` or `AWS_ACCOUNT_ID`. Every placeholder is explained where it first appears.
3. **Environment variables** are referenced as `VAR_NAME`. Their defaults are documented in
   `configuration-reference.md` and `.env.example`.
4. Every long-running command (servers, compose, terraform apply) is **explained before** it is run.
5. Where a command mutates infrastructure, the **rollback** or **undo** steps are listed in the same document.
6. Paths are relative to the repository root (`/workspace` on a dev machine; `<repo>/` in general) unless stated otherwise.

---

## 5. Quick start

### 5.1 Local development

```bash
# 1. Start the infrastructure containers (Postgres, Redis, Kafka, MinIO, Mailpit)
docker compose -f infra/docker/docker-compose.yml up -d

# 2. Run the whole platform (builds jars, starts all 19 services in order)
scripts/run-services.sh

# 3. Run the recruiter portal
scripts/run-recruiter-portal.sh
```

See `development/README.md` for prerequisites and detailed verification.

### 5.2 Production deployment

```bash
# 1. Create the AWS account and IAM user  -> deployment/01 + 02
# 2. Configure the AWS CLI               -> deployment/03
# 3. Install Terraform, kubectl, Helm    -> deployment/04, 05, 06
# 4. Bootstrap remote state              -> deployment/07
# 5. Provision infrastructure            -> deployment/08
# 6. Build and push images to ECR        -> deployment/09, 10
# 7. Deploy Kafka, PostgreSQL, Redis     -> deployment/11, 12, 13, 14
# 8. Deploy services and ingress         -> deployment/15, 16
# 9. Verify, domain, HTTPS, checklist    -> deployment/17, 18, 19, 20
```

---

## 6. Support resources

| Resource | Location |
|---|---|
| Configuration reference | `docs/configuration-reference.md` |
| API documentation | `docs/api.md` |
| Docker Compose stack | `infra/docker/docker-compose.yml` |
| Kubernetes manifests | `infra/k8s/` |
| Helm chart | `infra/helm/interview-integrity/` |
| Terraform roots | `terraform/environments/{local,dev,qa,uat,prod}` |
| CI/CD workflows | `.github/workflows/{ci,terraform,deploy}.yml` |
