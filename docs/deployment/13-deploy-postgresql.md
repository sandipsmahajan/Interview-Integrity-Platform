# 13 — Deploy PostgreSQL (databases and users)

**Purpose.** To provision PostgreSQL with the 16 platform databases and the per-service database
users that the application connects as.

## Prerequisites

- **dev/local**: EKS cluster ready (step 08), and no prior postgres StatefulSet.
- **qa/uat/prod**: RDS provisioned in step 08 (Terraform output `db_endpoint` +
  `db_master_secret_arn`).
- `kubectl` configured for the environment.

## Estimated Time

- dev/local: 15 minutes.
- qa/uat/prod: 10 minutes (RDS already exists; this step only creates databases/users).

## Required AWS permissions

- dev/local: cluster write (kubectl).
- prod: `rds:CreateDBInstance` was already exercised in step 08; running SQL against RDS requires
  the master password from Secrets Manager (readable by your IAM role).

## Which path applies to you

| Environment | PostgreSQL | Who creates databases |
|---|---|---|
| `local` | Docker container | compose init script |
| `dev` | In-cluster Postgres StatefulSet | `infra/k8s/postgres.yaml` init container |
| `qa`/`uat`/`prod` | Amazon RDS | this step (idempotent SQL via a job) |

## The 16 databases

```text
identity_db organization_db recruiter_db candidate_db interview_db
telemetry_db policy_db report_db notification_db analytics_db audit_db
storage_db feature_flag_db scheduler_db integration_db configuration_db
```

Each belongs to exactly one service (see [`database.md`](../architecture/database.md)). Flyway
migrations run per service at startup; this step only creates the databases and users.

## Step A — dev/local: apply the StatefulSet

```bash
kubectl apply -f infra/k8s/postgres.yaml
```

**What this does:**

- Creates a **StatefulSet** named `postgres` (stable network identity: `postgres-0`), its headless
  Service `postgres` (so services connect to `postgres:5432`), a PVC (`gp3-encrypted` StorageClass
  — see `infra/k8s/storageclass.yaml`), a `postgres` credentials Secret, and an init
  **ConfigMap**.
- The init container runs an idempotent script: for each of the 16 database names it checks
  `pg_database` and runs `CREATE DATABASE` only if missing. It also creates the `integrity` user
  and grants it rights on every database.

Wait for it:

```bash
kubectl -n integrity rollout status statefulset/postgres
kubectl -n integrity get pods -l app=postgres
```

## Step B — qa/uat/prod: create databases on RDS

RDS gives you one empty server; the databases must be created exactly once. Do this from a
one-off pod in the cluster (never from your laptop):

```bash
# 1. Run an interactive postgres client pod in the cluster
kubectl -n integrity run pg-admin --rm -it \
  --image=postgres:16 --restart=Never -- \
  bash -c 'echo "client ready; psql commands below"'
```

Then, with the master password (from Secrets Manager, secret `integrity/<env>/rds-master`), run:

```sql
CREATE DATABASE identity_db;
CREATE DATABASE organization_db;
-- ... and so on for all 16 databases listed above
```

To keep it repeatable, wrap the creation in the same idempotent script the dev StatefulSet uses
(the `init-db` ConfigMap in `infra/k8s/postgres.yaml` is the reference implementation) and run it
as a Kubernetes **Job**:

```bash
# Render and run the job (the manifest defines the 16-database loop)
kubectl apply -f infra/k8s/postgres-init-job.yaml   # if you create one; see below
```

> **Recommendation:** add an `rds-init` Job manifest (Job that runs the idempotent
> `CREATE DATABASE IF NOT EXISTS`-style loop against the RDS endpoint) to `infra/k8s/` so the
> exact database list is versioned. The dev StatefulSet's init ConfigMap is the source of truth
> for the list.

### Create the per-service users (least privilege)

```sql
CREATE ROLE identity_service LOGIN PASSWORD '<strong-password>';
GRANT CONNECT ON DATABASE identity_db TO identity_service;
GRANT ALL PRIVILEGES ON DATABASE identity_db TO identity_service;
-- repeat per service; one role per database, no cross-database grants
```

Store each role's password in Secrets Manager (`integrity/<env>/db/<service>`) so the deploy
pipeline can materialize the `integrity-secrets` Secret.

## Expected output

- dev: `postgres` StatefulSet `1/1 Ready`; 16 databases present.
- RDS: databases and roles created; services can connect.

## Verification steps

### dev/local

```bash
kubectl -n integrity exec -it postgres-0 -- psql -U integrity -d postgres -c \
  "SELECT datname FROM pg_database ORDER BY datname;"
```

You should see the 16 databases plus `postgres`/`template0`/`template1`.

### RDS

Port-forward or exec the admin pod, then run the same `SELECT datname` query against the RDS
endpoint. You should see the 16 databases.

### End-to-end (both)

Start one service and confirm Flyway ran:

```bash
kubectl -n integrity logs deploy/identity-service --tail=100 | grep -i flyway
# Migrating schema "public" to version "1 - create identity schema" -> success
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `Database "identity_db" does not exist` | Databases never created | Run the idempotent creation script again |
| `Role "identity_service" does not exist` | Users not created | Create roles before starting services |
| `Password authentication failed for user` | Wrong password in secret | Rotate the DB password in Secrets Manager + restart pods |
| `connection to server at ... failed: timeout expired` | Security group blocks 5432 | Allow app SG → RDS SG on 5432 |
| PVC stuck `Pending` | No StorageClass / CSI driver | Check `kubectl get sc` shows `gp3-encrypted`; verify EBS CSI driver installed |

## Rollback procedure

- **dev/local**: `kubectl delete -f infra/k8s/postgres.yaml` (data volumes are **Retain**
  StorageClass — delete the PVCs separately only if you intend to wipe data).
- **RDS**: never delete databases casually. Dropping a database is irreversible; to "roll back"
  use a **restore from snapshot** into a new instance (`disaster-recovery.md`).

## Best practices

- Idempotent creation scripts (check-before-create) so re-runs are safe.
- One role per service, granted only on its own database — enforce in code, not memory.
- Keep database creation versioned: dev's init ConfigMap is the canonical 16-name list; keep the
  RDS init Job in sync with it.

## Security notes

- RDS is in private subnets with deletion protection and encryption at rest — do not weaken
  either.
- Connection strings point at the RDS endpoint with TLS (`ssl=true` on the JDBC URL in prod).
- Database passwords are in Secrets Manager, injected at deploy time, and never in Git.
