# Runbook: Scaling

**Symptom.** Traffic or load exceeds current capacity: sustained high CPU on pods, HPA pinned at
max replicas, or nodes over 80% allocatable. Users may see latency spikes.

**Severity.** P2 (planned scaling) / P1 (capacity emergency).

**Impact.** Latency and error-rate degradation until capacity catches up.

## Prerequisites

- `kubectl`, Terraform access, approval for prod scaling.
- 15 minutes.

## Decisions — what to scale first

| Observation | Scale |
|---|---|
| Pod CPU near limits but node capacity fine | HPA max replicas (services) |
| Nodes > 80% allocatable, pods Pending | Node group (cluster) |
| Kafka consumer lag growing | Consumer replicas (`kafka-failure.md`) |
| RDS connections/IOPS near limits | RDS instance class (DB scale-up) |
| Redis evictions / CPU | ElastiCache node type (`redis-failure.md`) |

## Resolution

### A — Scale a service (HPA bounds via chart values)

```bash
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --reuse-values \
  --set <service>.autoscaling.minReplicas=3 \
  --set <service>.autoscaling.maxReplicas=10
```

**What this does:** changes the HPA min/max; HPA continues to drive replicas by CPU.
For an immediate one-off bump (not persisted):

```bash
kubectl -n integrity scale deployment/<service> --replicas=5
# note: a later helm upgrade reverts this — prefer the values-based change
```

### B — Scale the cluster (node group, via Terraform)

```bash
cd terraform/environments/<env>
# edit terraform.tfvars:
#   eks_node_desired_size = <new>
#   eks_node_max_size     = <new-max>
terraform plan
terraform apply
kubectl get nodes -w   # watch new nodes join
```

### C — Scale down (after the peak)

Reverse the same operations. For nodes, the cluster-autoscaler shrinks empty nodes once the
scale-down delay passes; do not force-delete nodes.

## Verification

```bash
kubectl -n integrity get hpa            # replicas within new bounds, CPU < target
kubectl -n integrity get pods -o wide   # spread across nodes
kubectl get nodes                       # Ready, below 80% allocatable
kubectl -n integrity get deploy | grep -c '1/1'   # still 19 healthy
```

## Rollback

- Scale back to the previous values with the same `helm upgrade` / Terraform apply.
- If scaling *up* exposed a capacity limit elsewhere (e.g. DB connection ceiling), scale that
  component instead and review the SLO.

## Prevention

- Keep HPA max within node-group capacity so scale-out never collides with node limits.
- Review capacity in the weekly ops review (CPU headroom, Kafka lag baseline).
- Load-test at 1.5× expected peak quarterly (see `monitoring.md` SLO section) so scaling targets
  are evidence-based, not guesses.
