# Backup & Disaster Recovery

This document describes the backup strategy and disaster-recovery (DR)
objectives for the Integrity Pro platform across runtimes, aligned with AWS
Well-Architected reliability guidance.

## RPO / RTO Targets

| Environment | RPO | RTO |
| --- | --- | --- |
| dev | 1 day (informal) | 1 day |
| qa | 15 minutes (PITR) | 2 hours |
| uat | 15 minutes (PITR) | 1 hour |
| prod | 5 minutes (PITR) | 30 minutes |

## Component Backup Matrix

| Component | Strategy | Owner |
| --- | --- | --- |
| PostgreSQL (RDS) | automated snapshots + PITR (30d prod) | AWS / Terraform |
| ElastiCache Redis | snapshot + automatic failover | AWS / Terraform |
| Amazon MSK | replication across brokers (3 AZs) | AWS |
| S3 objects | versioning + lifecycle | AWS / Terraform |
| ECR images | immutable tags + lifecycle (20/image) | AWS / Terraform |
| EKS manifests | git (Helm charts) | repo |
| Terraform state | S3 versioned + DynamoDB locked | AWS / Terraform |

## Database (RDS)

- **Automated backups** run daily; retention 7–30 days depending on
  environment. **PITR** restores to any point within the window.
- **Manual snapshots** before major migrations/upgrades.
- **Cross-region**: enable `backup_plan` copy to the DR region (us-west-2)
  for prod; the Terraform module exposes the flag for this.
- **Restore procedure**:
  1. `aws rds restore-db-instance-from-db-snapshot` (or `--use-latest-restorable-time`).
  2. Wait for `available`.
  3. Point the application at the new endpoint (DNS/parameter change).
  4. Re-run Flyway only if restoring to a schema older than current.

## Object Storage (S3)

- Versioning enabled on `documents`, `reports`, `uploads`.
- Lifecycle: objects → `STANDARD_IA` (90d) → `GLACIER` (180d); non-current
  versions expire at 30 days; `flowlogs`/`alb-logs` expire 30/90 days.
- **S3 Cross-Region Replication** can be enabled per bucket for prod if an RPO
  on objects is required (currently not enabled; documented as an option).

## ElastiCache Redis

- Redis **AOF** persistence with automatic snapshots (default 1/hour).
- **Multi-node (qa/uat/prod)** clusters use automatic failover with replica
  promotion; reads fail over to the promoted primary.
- ElastiCache is a cache, not the system of record — recovery restores from
  source systems.

## Kafka (MSK)

- MSK runs 2–3 brokers across AZs; replication factor ≥ 3 for topics
  (`offsets.topic.replication.factor`). Topic data is durable on EBS with
  KMS encryption.
- **Log retention** is intentionally bounded (168h / 1GiB per topic for dev;
  configurable per topic in prod) because events are not the system of record —
  consumers rebuild state from PostgreSQL. For strict event sourcing, raise
  retention and enable tiered storage.

## Kubernetes / Application

- Helm chart + `infra/config` are in git — the entire platform can be
  re-deployed with `helm upgrade --install`.
- **StatefulSet (dev Postgres)**: PVC on `gp3-encrypted` with `Retain`
  reclaim policy; back up with `pg_dump` on a schedule (cron) or Velero +
  Restic.
- **Velero** is recommended for cluster-level DR (namespaces, PVCs); the
  manifests can be applied from `infra/k8s/` and the Helm chart.

## Terraform State

- State stored in `integrity-terraform-state` S3 bucket (versioned,
  KMS-encrypted, public access blocked).
- Locking via DynamoDB `integrity-terraform-locks` prevents concurrent applies.
- Bucket lifecycle expires non-current versions after 90 days.
- `terraform plan` runs on every PR; apply is gated per environment.

## DR Runbook (prod region loss)

1. **Fail DNS** — Route53 records point at the prod ALB; in a region loss,
   update aliases to a pre-provisioned standby in `us-west-2` (documented as an
   option; not enabled by default).
2. **Restore data** — RDS cross-region snapshot/backup copy, S3 CRR, Redis
   rebuild from source.
3. **Re-provision** — run Terraform in the DR region with
   `tfvars.dr.tfvars`, then deploy via the CD pipeline
   (`workflow_dispatch` with the DR environment).
4. **Validate** — smoke test the API gateway health and the smoke test job in
   the deploy workflow.

## Ongoing Verification

- **Restore drills** for RDS (prod at least quarterly): restore from the latest
  snapshot to a scratch instance and run the smoke tests.
- **Backup alarms** — CloudWatch alerts if a backup job fails or RDS backup
  status degrades (`monitoring` module).
- **`aws backup` plans** can centralise tagging-based backup policies across
  RDS/EBS if the organisation standard requires it.
