# Troubleshooting: Database Connection Issues

**Symptom.** Services log connection errors to PostgreSQL: `Connection refused`, `timeout`,
`password authentication failed`, `too many connections`, or slow-to-connect.

## 1. `Connection refused` / `timeout expired`

**Diagnose:**

```bash
# Is the DB reachable from inside the cluster?
kubectl -n integrity run db-check --rm -it --image=postgres:16 -- \
  pg_isready -h <db-host> -p 5432

# Dev: in-cluster postgres pod healthy?
kubectl -n integrity get pods -l app=postgres
# Prod: RDS state?
aws rds describe-db-instances --query 'DBInstances[].{DB:DBInstanceIdentifier,Status:DBInstanceStatus}' --output table
```

| Result | Cause | Fix |
|---|---|---|
| `no response` / timeout | Security group or network policy blocks 5432 | Allow app SG → DB SG; check `kubectl -n integrity get netpol` allows egress 5432 |
| `connection refused` | DB down / wrong host in config | Start RDS (`runbooks/database-failure.md` §A); fix `DB_HOST` in the profile config |
| Wrong host | Config points at another env's DB | Check `infra/config/application-<env>.yml` datasource URL |

## 2. `password authentication failed for user "identity_service"`

**Cause:** password mismatch between the DB role and `integrity-secrets` (rotation drift or a
manual change on one side).

**Fix:**

```bash
# Confirm the secret value
aws secretsmanager get-secret-value --secret-id integrity/<env>/db/identity-service
# Confirm the DB role password (as master, via admin pod)
ALTER USER identity_service PASSWORD '<value-from-secret>';
# or re-sync the secret to the DB, then rollout
kubectl -n integrity rollout restart deployment/identity-service
```

See `runbooks/password-rotation.md`.

## 3. `FATAL: too many connections for role`

**Cause:** connection pool exhaustion. All services share the RDS connection budget.

**Fix (in order):**

1. Reduce Hikari pool sizes per service (config) — the platform's services each have their own
   DB but share the instance's connection ceiling:
   ```bash
   # infra/config/application-<env>.yml -> spring.datasource.hikari.maximum-pool-size
   kubectl -n integrity delete configmap integrity-config
   kubectl -n integrity create configmap integrity-config --from-file=infra/config/application-<env>.yml
   kubectl -n integrity rollout restart deployment --all
   ```
2. Terminate idle connections (emergency, master session):
   ```sql
   SELECT pg_terminate_backend(pid) FROM pg_stat_activity
   WHERE state='idle' AND datname NOT IN ('postgres','template0','template1');
   ```
3. Scale the RDS instance up (`runbooks/scaling.md` §C).

## 4. Intermittent timeouts / slow connects

**Diagnose:**

```bash
# From a pod, measure connect time
kubectl -n integrity run db-check --rm -it --image=postgres:16 -- \
  time pg_isready -h <db-host> -p 5432
# CloudWatch: DatabaseConnections, CPUUtilization, ReadLatency/WriteLatency
```

| Cause | Fix |
|---|---|
| CPU saturated | Scale instance class |
| I/O latency | Check `ReadIOPS`/`ReadLatency`; move to gp3 or larger |
| Connection churn (pool too small + frequent new connections) | Increase Hikari max pool size slightly, or set `initialization-fail-timeout` sanely |
| DNS resolution slow | Use the endpoint directly; no NAT hops in config |

## 5. `SSL off` / TLS warning on RDS

**Cause:** the JDBC URL lacks `ssl=true` (prod should enforce TLS).

**Fix:** set `?ssl=true&sslmode=verify-full` in the datasource URL for qa/uat/prod and rollout.

## 6. Migrations vs connections

If the same error accompanies Flyway failures, see `flyway-failures.md` first — a stuck failed
migration can hold connections and mask itself as a connection issue.

## Prevention

- Alarms: `DatabaseConnections > 80%`, `FreeStorageSpace < 20%`, CPU > 80%.
- Keep pool sizes documented per service (see `configuration-reference.md`).
- Test password rotation in qa before prod.
