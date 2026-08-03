# Infrastructure Architecture

This document describes how the Integrity Pro platform runs on Amazon EKS and
how the same application artifacts run across four runtimes with zero Java code
changes. Everything is configuration-driven.

## Runtime Matrix

| Runtime | Infra | Spring profile | Kafka | PostgreSQL | Redis | Object storage | Mail |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Local (IDE) | Docker Compose infra | `local` | Docker Kafka | Docker | Docker | MinIO | Mailpit |
| Docker Compose | all in `infra/docker/docker-compose.yml` | `docker` | Compose Kafka | Compose | Compose | MinIO | Mailpit |
| Kubernetes dev | `infra/k8s/*.yaml` + Helm | `kubernetes` | Strimzi | StatefulSet | Deployment | MinIO | Mailpit |
| EKS `dev` | Terraform + Helm | `dev` | Strimzi | RDS | ElastiCache | MinIO | Mailpit |
| EKS `qa` | Terraform + Helm | `qa` | MSK | RDS | ElastiCache | S3 | SES |
| EKS `uat` | Terraform + Helm | `uat` | MSK | RDS | ElastiCache | S3 | SES |
| EKS `prod` | Terraform + Helm | `prod` | MSK | RDS | ElastiCache | S3 | SES |

The application image is identical everywhere. The differences live entirely in:

1. `infra/config/application-<profile>.yml` — externalised Spring configuration.
2. Environment variables (`DB_HOST`, `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, ...).
3. Terraform state (what AWS resources exist).

## Topology (production)

```
                     Internet
                        |
                 Route53 (api.integritypro.example.com)
                        |
                 ACM TLS certificate
                        |
                WAF (web ACL, prod)
                        |
   Application Load Balancer (internet-facing, dual AZ)
                        |
              nginx ingress controller (NodePort 30080)
                        |
                  api-gateway :8080
                        |
   +----------------+---+----------------+-------------+
   |                |   |                |             |
Eureka discovery  identity  organization  interview  ... 18 services
(service registry)
   |                |   |                |
 RDS PostgreSQL  ElastiCache Redis   Amazon MSK (SASL/SCRAM)
   (multi-AZ)      (auto-failover)     |
                                        +--> S3 (documents/reports/uploads)
                                        +--> SES (email)
```

## Terraform Layout

```
terraform/
  bootstrap/               # state bucket + DynamoDB locks + KMS key (run once)
  account/                 # GitHub OIDC provider (run once, account-scoped)
  environments/
    local/  dev/  qa/  uat/  prod/      # one isolated root per environment
  modules/
    shared  security-groups  kms  iam  vpc  networking  s3  ecr
    rds  redis  kafka  secrets-manager  parameter-store  eks  alb
    acm  route53  cloudwatch  monitoring
```

Each environment root is a complete, independently stateful stack (remote state
in `integrity-terraform-state`, lock table `integrity-terraform-locks`, key
`integrity/<env>/terraform.tfstate`). Environments differ only in size and
feature flags:

| | local | dev | qa | uat | prod |
| --- | --- | --- | --- | --- | --- |
| NAT gateways | 1 | 1 | 1 | 3 | 3 |
| Interface VPC endpoints | off | off | on | on | on |
| EKS node group | 1-3 m6i.large | 2-6 m6i.large | 3-9 m6i.large | 3-12 m6i.large | 6-15 m6i.large |
| RDS class | db.t4g.small | db.t4g.medium | db.t4g.medium | db.m6i.large | db.m6i.xlarge |
| RDS multi-AZ | no | no | no | yes | yes |
| RDS backup days | 7 | 14 | 14 | 14 | 30 |
| Redis | t4g.micro x1 | t4g.micro x1 | t4g.small x2 | t4g.small x3 | m6g.large x3 |
| MSK (Kafka) | off | off | on | on | on |
| WAF | off | off | off | off | on |
| PITR / deletion protection | no | no | yes | yes | yes |

## Kubernetes / Helm Layout

- `infra/k8s/` — data-plane manifests used by the `kubernetes` profile:
  namespaces, storage class, PostgreSQL StatefulSet, Redis, MinIO, Mailpit and
  the Strimzi Kafka cluster + topics.
- `infra/helm/interview-integrity/` — umbrella Helm chart that renders all 19
  microservices (Deployment, Service, ServiceAccount, HPA, PDB), the nginx
  Ingress and namespace network policies. Per-environment values:
  `values-local/dev/qa/uat/prod.yaml`.

Externalised configuration is a single source of truth in `infra/config/`. The
deployment pipeline builds the `integrity-config` ConfigMap from it and the
Helm chart mounts it at `/etc/integrity/config/` in every pod. Secrets are
injected separately (`integrity-secrets`, sourced from GitHub environment
secrets and AWS Secrets Manager).

## Network Topology

- **VPC**: 3 AZs, public + private subnets. Private subnets host EKS nodes,
  RDS, ElastiCache and MSK. NAT gateways provide outbound egress; qa/uat/prod
  use one per AZ.
- **Security groups**: dedicated per component; the EKS node SG is the single
  ingress source for RDS/ElastiCache/MSK.
- **Gateway endpoints**: S3 + DynamoDB (no NAT cost for state/registry).
- **Interface endpoints** (uat/prod): ECR API/DKR, Secrets Manager, SSM,
  CloudWatch Logs, STS — no public egress for the data plane.
- **VPC flow logs** to `integrity-<env>-flowlogs` (S3, 90-day retention).
- **Network policies**: default-deny in the `integrity` namespace with
  selective egress (intra-namespace, Strimzi 9092, DNS, managed data plane).

## Observability

- CloudWatch log groups per service with ERROR metric filters.
- Prometheus scrapes `/actuator/prometheus` (annotations added by the chart).
- CloudWatch dashboard `integrity-<env>-overview`.
- CloudWatch alarms (SNS → email): RDS CPU/connections/storage, ElastiCache
  CPU/memory, MSK broker CPU, ALB 5xx / unhealthy hosts.

## CI/CD

- `.github/workflows/ci.yml` — backend, rust client, portals + infra validation
  (terraform fmt/validate, helm lint/template, YAML parse).
- `.github/workflows/terraform.yml` — OIDC-based plan (PR) / apply (push →
  dev; workflow_dispatch → qa/uat/prod, gated by GitHub environments).
- `.github/workflows/deploy.yml` — build 19 images → ECR, apply ConfigMap +
  Secret, `helm upgrade`, rollout + smoke test, rollback on failure.

See `terraform/README.md` for the bootstrap steps and the GitHub environment
setup requirements.
