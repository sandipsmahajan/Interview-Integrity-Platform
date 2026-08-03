# Architecture Documentation

This directory documents how Integrity Pro is designed and why. It is the first stop for any
engineer who needs to understand the platform before deploying, operating, or extending it.

## Index

| Document | Contents |
|---|---|
| [`high-level.md`](high-level.md) | The platform in one diagram: users, portals, API gateway, services, data plane, client. |
| [`microservices.md`](microservices.md) | The 19 microservices, their responsibilities, ports, and inter-service calls. |
| [`infrastructure.md`](infrastructure.md) | The AWS infrastructure: accounts, environments, VPC, EKS, RDS, MSK, ElastiCache, S3, Secrets Manager. |
| [`networking.md`](networking.md) | VPC layout, subnets, security groups, ALB → ingress → service networking, network policies. |
| [`deployment.md`](deployment.md) | Deployment topology: images, ECR, Helm, ingress front door, rollback path. |
| [`sequence.md`](sequence.md) | Sequence diagrams: interview lifecycle, telemetry ingestion, candidate onboarding, secrets rotation. |
| [`database.md`](database.md) | The 16 PostgreSQL databases, schema ownership, connections, and migrations. |
| [`kafka.md`](kafka.md) | Kafka topology: Strimzi in dev/local, MSK in qa/uat/prod, topics, consumers, replay, DLQ. |
| [`monitoring.md`](monitoring.md) | Metrics, logs, tracing and alerting architecture (Prometheus/Grafana, CloudWatch, OTel, Jaeger). |
| [`security.md`](security.md) | Threat model, IAM, TLS, network policies, secrets, encryption, RBAC. |
| [`decisions.md`](decisions.md) | Every architectural design decision and the rationale behind it (ADR-style log). |

## Core facts to remember

1. **One codebase, many environments.** The same 19 services run in `local`, `dev`, `qa`, `uat` and
   `prod`. Environment differences are configuration only — `SPRING_PROFILES_ACTIVE` plus
   environment variables using the `${ENV_VAR:default}` pattern.
2. **The API gateway is the only public front door.** All browser, portal and client traffic enters
   through `api-gateway` on port 8080. Portals are served separately by the ingress.
3. **PostgreSQL hosts 16 logical databases**, one per service. Kafka carries events. Redis is the
   cache. MinIO/S3 is object storage.
4. **Everything is code.** Infrastructure is Terraform; workloads are Helm; the delivery is GitHub
   Actions with OIDC — no click-ops.
5. **Secure by default.** Default-deny network policies, TLS everywhere, secrets in AWS Secrets
   Manager, encryption at rest on every data store.
