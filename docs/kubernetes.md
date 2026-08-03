# Kubernetes Guide

**Purpose.** The working reference for the Kubernetes objects Integrity Pro uses: namespaces,
deployments, services, ingress, ConfigMaps, secrets, persistent storage, Helm, autoscaling,
rolling updates, and rollbacks — all in the context of this platform.

> The "what is where" map: namespaces in `infra/k8s/namespace.yaml`, data-plane apps in
> `infra/k8s/*.yaml`, the 19 services via the Helm chart `infra/helm/interview-integrity`.

## 1. Namespaces

| Namespace | Contains |
|---|---|
| `integrity` | The 19 microservices + data-plane apps (Postgres, Redis, MinIO, Mailpit in dev) |
| `kafka` | Strimzi operator + Kafka broker (dev/local only) |
| `ingress-nginx` | The ingress controller |
| `kube-system` | Cluster control-plane components (don't touch) |

```bash
kubectl get namespaces
kubectl -n integrity get all     # everything in the platform namespace
```

**Why namespaces?** Isolation of control (RBAC, quotas, network policies) and a clean blast
radius. The platform namespace is default-deny for network traffic (see `networking.md` §5).

## 2. Deployments

A Deployment manages identical pods and guarantees the desired replica count.

```bash
kubectl -n integrity get deployments
kubectl -n integrity describe deployment identity-service
kubectl -n integrity get pods -l app=identity-service
```

The Helm chart renders one Deployment per service with:

- **image**: `<account>.dkr.ecr.us-east-1.amazonaws.com/integrity-<env>/<service>:<sha>`
- **resources**: requests (guaranteed) and limits (ceiling)
- **probes**: readiness `/actuator/health/readiness`, liveness `/actuator/health/liveness`
- **envFrom**: `integrity-config` (ConfigMap) + `integrity-secrets` (Secret)
- **checksum annotation**: changes to config/secret trigger a new rollout

### The probes explained

| Probe | URL | If it fails |
|---|---|---|
| readiness | `/actuator/health/readiness` | Pod removed from Service endpoints (no traffic) |
| liveness | `/actuator/health/liveness` | Pod restarted by kubelet |

**Rule:** never make liveness depend on external systems (DB, Kafka). A flaky DB would otherwise
cause a restart loop. That is why liveness is the *process* check and readiness is the *dependency*
check.

## 3. Services

A Service is a stable network endpoint in front of pods.

```bash
kubectl -n integrity get svc
# api-gateway      ClusterIP  10.x.x.x   8080/TCP
# identity-service ClusterIP  10.x.x.x   8081/TCP
```

| Type | Where used |
|---|---|
| `ClusterIP` | All 19 services (internal only) |
| `NodePort` | ingress-nginx (`30080`) — the ALB targets this |
| `Headless` | Postgres StatefulSet (`postgres`) |

Pods find each other via CoreDNS by Service name: inside the cluster `http://identity-service`
resolves to the Service. Names and ports are fixed (`microservices.md` §1).

## 4. Ingress

```bash
kubectl -n integrity get ingress
kubectl -n integrity describe ingress integrity-api
```

The chart creates `integrity-api` (→ `api-gateway`) and `integrity-portal` (→ portal). Routing
rules are `Host`-based. The controller is ingress-nginx; the front door is the ALB (step 16).

## 5. ConfigMaps

```bash
kubectl -n integrity get configmap
kubectl -n integrity get configmap integrity-config -o yaml
```

`integrity-config` carries the non-secret profile config (`infra/config/application-<env>.yml`).
It is mounted read-only into every pod; the deployment's checksum annotation causes a rolling
restart when it changes.

## 6. Secrets

```bash
kubectl -n integrity get secret
```

`integrity-secrets` holds `JWT_SECRET`, `RDS_PASSWORD`, `KAFKA_SCRAM_USER/PASSWORD`,
`REDIS_PASSWORD`, MinIO keys. It is created by the deploy pipeline from Secrets Manager / GitHub
secrets — never stored in Git or Helm values.

```bash
# Create/recreate it manually only in emergencies (values still end up in shell history - avoid)
kubectl -n integrity create secret generic integrity-secrets --from-literal=...
```

Secrets are base64-encoded in the API (not encrypted at rest by default) — EKS encryption at
rest (KMS) protects them, and RBAC restricts who can read them.

## 7. Persistent volumes (PV) and claims (PVC)

```bash
kubectl get sc              # StorageClass list
kubectl -n integrity get pvc
```

| StorageClass | Provisioner | Use |
|---|---|---|
| `gp3-encrypted` | EBS CSI driver | All platform PVCs |

`infra/k8s/storageclass.yaml` defines a `gp3-encrypted` class: `WaitForFirstConsumer`
(volume is created in the AZ where the pod lands), `gp3` volume type, KMS-encrypted, `Retain`
(volume survives PVC deletion).

| Consumer | PVC | Contents |
|---|---|---|
| `postgres` (dev) | `data-postgres-0` | The 16 databases |
| `minio` (dev) | `minio-data` | Objects/buckets |
| `kafka` (dev) | ephemeral (Strimzi) | Topic data |

**Retain vs Delete:** `Retain` means `kubectl delete pvc` does not destroy the EBS volume — you
must delete the EBS volume (or re-attach it) yourself. This is intentional for data safety.

## 8. Helm (how it fits)

```bash
helm ls --namespace integrity
helm history integrity --namespace integrity
helm get values integrity --namespace integrity   # what's actually applied
```

- The chart is local (`infra/helm/interview-integrity`), values per env (`values-<env>.yaml`).
- Release name: `integrity`. Every `helm upgrade` creates a new revision; `helm rollback` reverts.

## 9. Horizontal Pod Autoscaler (HPA)

```bash
kubectl -n integrity get hpa
```

The chart creates an HPA per scalable service targeting **average CPU utilization** (default
~70%). HPA needs the **metrics-server**:

```bash
# If HPA reports FailedGetResourceMetric, install metrics-server:
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Check HPA behavior:

```bash
kubectl -n integrity describe hpa identity-service
# Metrics: ( current / target )  70% / 70%
# Min replicas / Max replicas:   2 / 6
```

**Rule:** HPA scale-out is driven by CPU today; add custom metrics (Kafka lag, queue depth) by
pointing HPA at a custom metrics API — see `monitoring.md`.

## 10. Resource limits

```bash
kubectl -n integrity get deploy identity-service -o jsonpath='{.spec.template.spec.containers[0].resources}'
```

Every container has:

- **requests** (CPU/memory): what the scheduler reserves and the HPA scales on.
- **limits**: the ceiling; exceeding memory → OOM kill; CPU is throttled.

**Why they matter:** a single telemetry burst in one pod cannot starve its node; the HPA has a
signal to scale on; the scheduler can pack pods correctly.

## 11. Rolling updates

Rolling updates are the default strategy. Chart settings: `maxUnavailable: 25%`,
`maxSurge: 25%`.

```bash
kubectl -n integrity rollout status deployment/identity-service
kubectl -n integrity rollout history deployment/identity-service
```

The checksum annotation makes config changes real rollouts. Because readiness gates new pods,
traffic switches only when the new pod is actually healthy.

## 12. Rolling rollbacks

```bash
# By deployment
kubectl -n integrity rollout undo deployment/identity-service
kubectl -n integrity rollout undo deployment/identity-service --to-revision=<n>

# By Helm release (the pipeline's preferred path)
helm rollback integrity <revision> --namespace integrity --reuse-values
```

See `runbooks/service-rollback.md` for the full procedure.

## 13. Scaling

### Manual

```bash
kubectl -n integrity scale deployment/identity-service --replicas=5
```

### Automatic

The HPA does this. Change HPA min/max via `values-<env>.yaml` (chart) — not by editing the HPA
directly (a `helm upgrade` would revert your change).

### Cluster (nodes)

Node count is managed by the EKS node group autoscaler from Terraform (`eks_node_desired_size` /
`min_size` / `max_size`). Resize by editing `terraform.tfvars` and applying — never by manually
adding nodes.

## 14. Upgrading the cluster

```bash
# 1. Update the pinned Kubernetes version in variables.tf
# 2. Plan + apply (EKS upgrades control plane, then node groups)
cd terraform/environments/<env>
terraform plan
terraform apply
# 3. Upgrade local tooling to a compatible minor
kubectl version --client
```

Sequence: upgrade **one minor at a time**, verify with `kubectl -n integrity get pods` (all
Ready) after each step, and upgrade `dev` → `qa` → `uat` → `prod` over separate maintenance
windows.

## 15. Troubleshooting map (quick)

| Symptom | Command | Doc |
|---|---|---|
| Pod Pending | `kubectl describe pod <p>` (look for PVC/CPU) | `runbooks/pod-crash.md` |
| Pod CrashLoop | `kubectl logs <p> --previous` | `runbooks/pod-crash.md` |
| ImagePullBackOff | `kubectl describe pod <p>` | `runbooks/pod-crash.md` |
| Service unreachable | `kubectl get endpoints <svc>` | `runbooks/ingress-issues.md` |
| HPA not scaling | `kubectl describe hpa <svc>` | `monitoring.md` |
| PVC stuck | `kubectl get pvc` / `kubectl get sc` | `kubernetes.md` §7 |
| Ingress 404 | `kubectl describe ingress` (Host rules) | `runbooks/ingress-issues.md` |

## Security notes

- RBAC: humans get scoped roles (read + targeted exec), never `cluster-admin`. The deploy
  pipeline uses the least-privilege ServiceAccounts rendered by the chart.
- The `integrity` namespace is protected by default-deny NetworkPolicy — understand it before
  opening anything.
- Encrypt-at-rest is configured at the EKS cluster level (KMS) and per-volume (`gp3-encrypted`);
  verify with `kubectl get pvc -o yaml` (volumeAttributes include the encrypted gp3 class).
