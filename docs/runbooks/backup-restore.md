# Runbook: Backup and Restore

**Symptom.** You need a backup (pre-change safety snapshot), a restore (data corruption), or a
point-in-time recovery (user error / bad migration).

**Severity.** Restore is P1. Backup is planned.

**Impact.** Data loss is permanent without backups; restore time determines RTO.

## Prerequisites

- `aws` CLI, Terraform outputs (endpoints/ARNs), cluster access.
- 30–60 minutes; restore requires a maintenance window.

## 1. Backup

### RDS automated backups (already on)

```bash
aws rds describe-db-instances --query \
  'DBInstances[].{DB:DBInstanceIdentifier,BackupRetention:BackupRetentionPeriod,PITR:LatestRestorableTime}' \
  --output table
```

RDS takes daily snapshots during the backup window and keeps PITR transaction logs for
`BackupRetentionPeriod` days (prod: 7+). **No action needed for routine backups.**

### Manual snapshot (before a risky change)

```bash
aws rds create-db-snapshot \
  --db-instance-identifier integrity-<env> \
  --db-snapshot-identifier integrity-<env>-pre-<change>-$(date +%Y%m%d%H%M)
```

### S3 (object storage) versioning (already on)

```bash
aws s3api get-bucket-versioning --bucket integrity-<env>-documents
# {"Status": "Enabled"}
```

Old versions are preserved automatically; delete markers let you restore an object that was
overwritten or deleted.

### Kafka topics (dev) — no durable backup needed

MSK retains by retention config; dev topics are code (CRs), replayable. Do not back up dev.

## 2. Restore

### Restore the latest automated snapshot (new instance)

```bash
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier integrity-<env>-restored \
  --db-snapshot-identifier <snapshot-id>
# wait for available
aws rds wait db-instance-available \
  --db-instance-identifier integrity-<env>-restored
```

### Point-in-time recovery (finest granularity)

```bash
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier integrity-<env> \
  --target-db-instance-identifier integrity-<env>-pitr \
  --restore-time <UTC timestamp e.g. 2026-08-03T10:00:00Z>
```

**What this does:** replays transaction logs up to (but not past) the requested time. Use it to
undo a bad migration that ran at a known moment.

### S3 object restore

```bash
# List versions of a deleted/corrupted object
aws s3api list-object-versions --bucket integrity-<env>-documents --prefix reports/
# Copy the wanted version back over the current key
aws s3api copy-object \
  --bucket integrity-<env>-documents \
  --copy-source integrity-<env>-documents/reports/<file>?versionId=<versionId> \
  --key reports/<file>
```

### Swap the app to the restored database

```bash
# Update the profile config (infra/config/application-<env>.yml) datasource URL to the restored
# instance, re-render ConfigMap, rollout:
kubectl -n integrity delete configmap integrity-config
kubectl -n integrity create configmap integrity-config \
  --from-file=infra/config/application-<env>.yml
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

## Verification

```bash
# Data is present and consistent
kubectl -n integrity run db-check --rm -it --image=postgres:16 -- \
  psql -h <restored-endpoint> -U <user> -d identity_db -c \
  "SELECT count(*) FROM users;"   # matches expectation

# Flyway schema is intact (no partial migration)
# -> flyway_schema_history shows success=t for the version before the bad change

# Full business smoke test (deployment/17-verify-platform.md)
```

## Rollback

- A restore that fails validation: restore again to a **newer/older** point, or fall back to the
  original instance (do not delete it until satisfied).
- If the swap to the restored DB breaks: point config back at the original endpoint and rollout.

## Prevention

- Quarterly restore drill (measure RTO; see `disaster-recovery.md`).
- Keep `LatestRestorableTime` monitored so you know your real PITR window.
- Alarms: failed RDS snapshot (`rds:snapshot-failure`), S3 versioning disabled.
