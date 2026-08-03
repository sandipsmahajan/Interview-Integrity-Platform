# Runbook: Service Rollback

**Symptom.** A release is causing errors, a bad config landed, or smoke tests fail after deploy.
You must return to the last known-good version.

**Severity.** P1 when the platform is degraded; P2 otherwise.

**Impact.** Service returns to the previous revision until the new release is fixed.

## Prerequisites

- `kubectl`, `helm`, the previous image SHA or Helm revision.
- 10 minutes.

## Decide the rollback method

| Situation | Method |
|---|---|
| Rolling release failed its smoke test | `helm rollback` (pipeline default) |
| One service is bad | `kubectl rollout undo` that deployment, or pin the old image SHA |
| Config/secret change caused the break | Revert config/secret + rollout (or rollback the Helm revision that carried it) |
| Data-plane change caused it (DB/Kafka/Redis) | Do **not** roll back the workload; restore data plane (`database-failure.md`, `backup-restore.md`) |

## Resolution

### A — Helm rollback (entire release)

```bash
# Find the last known-good revision
helm history integrity --namespace integrity
# REVISION  UPDATED   STATUS     CHART                    APP VERSION
# 4         12:00      deployed   interview-integrity-1.0.0  1.0.0
# 5         14:00      failed     interview-integrity-1.0.0  1.0.0   <- current (bad)

# Roll back to revision 4, preserving the applied values
helm rollback integrity 4 --namespace integrity --reuse-values

# Watch it come back
kubectl -n integrity rollout status deployment --all
```

**Why `--reuse-values`?** Without it Helm reuses the values file *from the chart*, which may
differ from what was applied. `--reuse-values` keeps the environment's actual values.

### B — Single-deployment rollback

```bash
kubectl -n integrity rollout history deployment/<service>
kubectl -n integrity rollout undo deployment/<service> --to-revision=<n>
kubectl -n integrity rollout status deployment/<service>
```

### C — Image pin (pin the previous SHA)

```bash
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --reuse-values \
  --set <service>.image.tag=<previous-sha>
```

## Verification

```bash
kubectl -n integrity get pods | grep <service>            # 1/1, new UID (image = old SHA)
kubectl -n integrity get deploy <service> -o jsonpath='{.spec.template.spec.containers[0].image}'
# .../integrity-<env>/<service>:<previous-sha>

# Smoke test the service through the gateway
curl -s https://api.<env>.../actuator/health | jq .status
# "UP"

# Confirm the symptom is gone (error rate returns to baseline in Grafana)
```

## Rollback of the rollback (re-promote)

Once the release is fixed, redeploy forward — never stay on an old revision for long (it may lack
security fixes):

```bash
# Re-run deploy.yml for the fixed commit, or:
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --reuse-values
```

## Prevention

- Deploy one environment at a time; verify before promoting (already in `deploy.yml`).
- Keep the deploy smoke test strict enough to catch regressions before rollout completes.
- Record the "last known-good revision" in the release notes so rollback is a lookup, not a
  hunt.
