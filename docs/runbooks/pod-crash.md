# Runbook: Pod Crash

**Symptom.** A pod is in `CrashLoopBackOff`, `ImagePullBackOff`, `OOMKilled`, or
`Error`. Users see 502/503 for the affected service.

**Severity.** P1 if a user-facing service; P2 otherwise.

**Impact.** Service unavailable or degraded; replicas may be 0.

## Prerequisites

- `kubectl` configured; access to the container logs.
- 15 minutes.

## Diagnosis

```bash
# What's wrong at a glance
kubectl -n integrity get pods
# NAME                    READY  STATUS             RESTARTS
# identity-service-abc    0/1    CrashLoopBackOff   5

# Why: events (reason + message)
kubectl -n integrity describe pod <pod-name>
```

The three common states:

| State | Meaning | Go to |
|---|---|---|
| `ImagePullBackOff` | Can't pull the image (missing tag, ECR auth, wrong digest) | §A |
| `OOMKilled` | Exceeded memory limit, kubelet killed it | §B |
| `CrashLoopBackOff` | App starts and exits (config, DB, code) | §C |

## §A — ImagePullBackOff

```bash
kubectl -n integrity describe pod <pod-name> | grep -A3 Events
# Failed to pull image ... repository does not exist / unauthorized / not found
```

Fix by cause:

```bash
# Tag missing from ECR (wrong SHA)?
aws ecr describe-images --repository-name integrity-<env>/<service> \
  --query 'imageDetails[].imageTags'

# Pod using a stale image? Set the correct tag via helm values and re-upgrade:
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --reuse-values

# Node role missing ECR pull permission?
# Re-apply the IAM module (terraform) or check the role policy has ecr:BatchGetImage
```

## §B — OOMKilled

```bash
kubectl -n integrity describe pod <pod-name> | grep -i -A2 'Last State'
# Reason: OOMKilled
# Exit Code: 137
```

Resolution:

1. Raise the memory **limit** in `values-<env>.yaml` for the service (e.g. `memory: 512Mi →
   1Gi`) and re-upgrade Helm.
2. Better: find the leak/peak first — `jvm_memory_used_bytes` in Grafana (see `monitoring.md`).
3. If it's a genuine leak, file a service ticket; raise limits as the *mitigation*.

```bash
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --reuse-values \
  --set <service>.resources.limits.memory=1Gi
kubectl -n integrity rollout status deployment/<service>
```

## §C — CrashLoopBackOff (app exits)

```bash
# What the app said right before dying
kubectl -n integrity logs <pod-name> --previous --tail=60
```

Common causes and fixes:

| Log signature | Cause | Fix |
|---|---|---|
| `Failed to configure a DataSource` | DB URL/creds wrong | Check ConfigMap/Secret; DB reachable (`database-failure.md`) |
| `Connection refused ... 9092` | Kafka unreachable | `kafka-failure.md` |
| `FlywayException: Validate failed` | Migration mismatch | `troubleshooting/README.md` → Flyway |
| `Unable to connect to Redis` | Redis down/auth | `redis-failure.md` |
| `No space left on device` | Disk | Check PVC/EBS usage |
| `ClassNotFoundException` | Image/jar mismatch | Rebuild the image (`deployment/09-*`) |

General fix path after fixing the root cause:

```bash
kubectl -n integrity rollout restart deployment/<service>
kubectl -n integrity rollout status deployment/<service>
```

## Verification

```bash
kubectl -n integrity get pods | grep <service>          # 1/1 Running, restarts low
kubectl -n integrity rollout status deployment/<service>
curl -s <service>.integrity:8080/actuator/health 2>/dev/null || \
  kubectl -n integrity exec -it <pod> -- curl -s localhost:8080/actuator/health
# {"status":"UP"}
```

## Rollback

If the crash started after a release, roll the release back immediately:

```bash
helm history integrity --namespace integrity
helm rollback integrity <revision> --namespace integrity --reuse-values
```

See [`service-rollback.md`](service-rollback.md).

## Prevention

- Run the image's health checks in the deploy pipeline smoke test (already done in `deploy.yml`).
- Watch OOM kills and restart counts in Grafana; alert on `restarts > 3/10min`.
- Never apply config/secret changes without the checksum-triggered rollout and a smoke test.
