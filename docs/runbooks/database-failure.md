# Runbook: Database Failure

**Symptom.** Services fail to connect to PostgreSQL; `connection refused`, `too many
connections`, slow queries, or `replication lag`. Users see 5xx on anything touching data.

**Severity.** P1 (platform-wide data dependency).

**Impact.** All 19 services that read/write data are affected.

## Prerequisites

- `kubectl`, `aws` CLI, Terraform access.
- RDS endpoint + master secret (Terraform outputs).
- 30 minutes.

## Diagnosis

```bash
# 1. Is RDS reachable? (dev/local: in-cluster postgres; prod: RDS)
kubectl -n integrity get pods -l app=postgres       # dev
aws rds describe-db-instances --query \
  'DBInstances[].{DB:DBInstanceIdentifier,Status:DBInstanceStatus,MultiAZ:MultiAZ,Storage:AllocatedStorage}' \
  --output table                                     # prod

# 2. Are services complaining?
kubectl -n integrity logs deploy/identity-service --tail=50 | grep -iE 'sql|connection|postgres'

# 3. Metrics
# CloudWatch: DatabaseConnections, CPUUtilization, FreeStorageSpace, ReadIOPS
# Grafana: Postgres dashboard (connections, cache hit, replication lag)
```

## Resolution by cause

### A — RDS instance stopped/failed

```bash
aws rds describe-db-instances --query 'DBInstances[].DBInstanceStatus'
# available | stopped | storage-full | failed

# If stopped, start it:
aws rds start-db-instance --db-instance-identifier integrity-<env>
# If storage-full or failed, see §C below.
```

Wait for `available`, then verify connectivity from a pod:

```bash
kubectl -n integrity run db-check --rm -it --image=postgres:16 -- \
  pg_isready -h <rds-endpoint> -p 5432
# <rds-endpoint>:5432 - accepting connections
```

### B — Connection exhaustion ("too many connections")

Connections are a shared resource. In order of preference:

```bash
# 1. Reduce per-service pool sizes via config (fastest safe fix):
#    edit infra/config/application-<env>.yml -> spring.datasource.hikari.maximum-pool-size
#    then recreate the ConfigMap + rollout
kubectl -n integrity delete configmap integrity-config
kubectl -n integrity create configmap integrity-config \
  --from-file=infra/config/application-<env>.yml
kubectl -n integrity rollout restart deployment --all

# 2. Terminate idle connections from the master session (emergency only):
#    (via the admin pod) SELECT pg_terminate_backend(pid) FROM pg_stat_activity
#    WHERE state='idle' AND datname NOT IN ('postgres','template0','template1');
```

### C — Storage full / slow

```bash
# Check space
aws rds describe-db-instances --query 'DBInstances[].AllocatedStorage'
# Modify to add storage (this is a scale-up; online for gp3)
aws rds modify-db-instance --db-instance-identifier integrity-<env> \
  --allocated-storage <current+20> --apply-immediately
```

For slow queries:

```sql
-- from the admin pod: find the top queries by total time
SELECT query, calls, total_exec_time, mean_exec_time
FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;
```

Add missing indexes (via a migration, not ad-hoc SQL), then tune `work_mem`/`shared_buffers` in
the RDS parameter group.

### D — Corruption / data issue (restore path)

Do **not** attempt in-place repair on prod. Restore from the latest snapshot/PITR into a new
instance and swap the endpoint. Full procedure: [`backup-restore.md`](backup-restore.md) and
`disaster-recovery.md`.

## Verification

```bash
kubectl -n integrity run db-check --rm -it --image=postgres:16 -- \
  pg_isready -h <rds-endpoint> -p 5432
# accepting connections

kubectl -n integrity logs deploy/identity-service --tail=30 | grep -i flyway
# "Migrating schema ... success"

# Business flow: run the login smoke test (deployment/17-verify-platform.md Level 4)
```

## Rollback

- Config/pool changes: revert the config file and re-apply (rollout restarts pods).
- A restore is itself a rollback path — record the restore point and keep the old instance
  (renamed, not deleted) until the team is confident.

## Prevention

- Alarms: `FreeStorageSpace < 20%`, `DatabaseConnections > 80% max`, `CPU > 80%`, `ReplicaLag`.
- Right-size Hikari pools per service (this platform's services connect to *their own* DB, so
  pool sizes are per service, not shared).
- Schedule index/`pg_stat_statements` review per release.
- Quarterly restore drill (see `backup-restore.md`).
