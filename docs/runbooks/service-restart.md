# Runbook: Service Restart

**Symptom.** A service is up but behaving incorrectly (stuck state, leaky connections, stale
config, unhealthy endpoints) and needs a clean restart.

**Severity.** P2 (P1 if a restart does not resolve a user-facing outage).

**Impact.** Brief blips for the restarted service's callers; the rolling restart keeps the
service available if replicas > 1.

## Prerequisites

- `kubectl` configured (10 minutes, cluster write).

## Diagnosis

Confirm which deployment and whether it's actually stuck:

```bash
kubectl -n integrity get deploy
kubectl -n integrity get pods
# Look for a pod that is Running but with high restarts, or a deployment whose
# available replicas < desired.

# Check the current state of one pod
kubectl -n integrity describe pod <pod-name>
```

If pods are `CrashLoopBackOff`/`ImagePullBackOff`, use [`pod-crash.md`](pod-crash.md) instead.
If the *whole platform* is down, use [`incident-response.md`](incident-response.md).

## Resolution

### Option A — restart the deployment (recommended, zero-downtime)

```bash
kubectl -n integrity rollout restart deployment/<service>
```

**What this does:** performs a rolling restart — new pods are created and must pass readiness
before old ones are removed. Traffic is never dropped as long as the new pods become ready.

```bash
kubectl -n integrity rollout status deployment/<service>
# deployment "<service>" successfully rolled out
```

### Option B — restart all services (only when required, e.g. secret rotation)

```bash
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

### Option C — delete a single stuck pod (lets the controller recreate it)

```bash
kubectl -n integrity delete pod <pod-name>
```

## Verification

```bash
kubectl -n integrity get pods | grep <service>          # 1/1 Running
kubectl -n integrity rollout status deployment/<service>  # successfully rolled out
kubectl -n integrity logs deploy/<service> --tail=30 | grep -iE 'started|error'
# "Started <Service>Application in x.xxx seconds" and no new errors
```

## Rollback

If the service is *worse* after restart, restore the previous image/revision:

```bash
kubectl -n integrity rollout undo deployment/<service>
```

Or via Helm (see [`service-rollback.md`](service-rollback.md)):

```bash
helm rollback integrity <revision> --namespace integrity --reuse-values
```

## Prevention

- Restarts are usually a symptom: check `kubectl describe pod` for the last state, memory/CPU
  spikes, and recent config/secret changes before concluding it was transient.
- Add a restart to the morning check log so patterns surface.
