# Runbook: Node Failure

**Symptom.** A node is `NotReady`, `MemoryPressure`/`DiskPressure`, or the autoscaler is not
growing/evacuating nodes. Pods may be rescheduling.

**Severity.** P2 (single-node loss is absorbed by replicas); P1 if multiple nodes fail or the
autoscaler is broken.

**Impact.** Pods on the node reschedule; if capacity is short, some pods stay `Pending`.

## Prerequisites

- `kubectl`, `aws` CLI.
- 15–30 minutes.

## Diagnosis

```bash
kubectl get nodes
# STATUS NotReady / MemoryPressure

kubectl describe node <node-name>
# Conditions, allocatable vs capacity, and Kubelet last contact

# Which pods were on it
kubectl get pods -A -o wide | grep <node-name>

# Node group health (AWS side)
aws eks describe-nodegroup --cluster-name integrity-<env>-eks \
  --nodegroup-name <ng> --query 'nodegroup.status'
```

## Resolution by cause

### A — Single NotReady node (transient)

```bash
# The node will drain on its own if unhealthy; force-recycle it:
kubectl delete node <node-name>
```

The node group autoscaler replaces it with a fresh instance (stateful pods on it should already
have been rescheduled by Kubernetes; verify no pod is stuck `Terminating`).

### B — Pressure conditions (disk/memory on the node)

```bash
kubectl describe node <node-name> | grep -A3 'Conditions'
# Check for DiskPressure from full EBS volumes or image bloat.
# Then evict properly and let it rebuild:
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data
kubectl delete node <node-name>
```

### C — Autoscaler not scaling

```bash
# Check node group min/max/desired from Terraform outputs:
cd terraform/environments/<env>
terraform output # eks_node_min/max/desired

# If pods are Pending due to insufficient capacity, scale the node group:
#   edit eks_node_desired_size in terraform.tfvars, then:
terraform plan && terraform apply
```

If the cluster-autoscaler itself is unhealthy:

```bash
kubectl -n kube-system get pods | grep autoscaler
kubectl -n kube-system logs deploy/cluster-autoscaler --tail=50
```

## Verification

```bash
kubectl get nodes            # all Ready
kubectl get pods -A -o wide  # no pods stuck on a deleted node
kubectl -n integrity get pods  # all 1/1
```

## Rollback

- A scaled-up node group: revert `eks_node_desired_size` and apply (autoscaler scales down
  empty nodes; respect its scale-down-delay).
- A wrongly deleted node: the node group recreates it automatically; if it didn't, trigger via
  `aws eks update-nodegroup-config` or Terraform.

## Prevention

- Alarms: node `NotReady`, `MemoryPressure`, `DiskPressure`.
- Keep node group min/max realistic so the autoscaler can actually heal (e.g. prod
  `min=6, max=15`).
- Pin workloads with `topologySpreadConstraints` (chart) so losing one node loses at most one
  replica per service.
