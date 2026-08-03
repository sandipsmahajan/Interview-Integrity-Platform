# 15 — Deploy the Services with Helm

**Purpose.** To deploy all 19 microservices to EKS using the umbrella Helm chart
(`infra/helm/interview-integrity`), pointed at the images you pushed in step 10.

## Prerequisites

- Steps 08–14 completed (cluster, data plane, Kafka, Postgres, Redis).
- Images in ECR (step 10).
- The `integrity` namespace exists (`kubectl get ns integrity`; if not:
  `kubectl apply -f infra/k8s/namespace.yaml`).
- ConfigMap `integrity-config` and Secret `integrity-secrets` exist. In the normal flow the
  `deploy.yml` pipeline creates them; for the first manual deploy you create them as described
  below.

## Estimated Time

20 minutes (plus image pull time).

## Required AWS permissions

Cluster write via kubectl; the cluster must be able to pull images from ECR (the node role's
ECR permissions from step 08 cover this).

## How the chart works

`helm upgrade` renders templates from `infra/helm/interview-integrity` using the environment's
`values-<env>.yaml` and creates/reconciles, per service:

- `Deployment` (image, probes, limits, `envFrom` config+secrets, checksum annotation)
- `Service` (ClusterIP, fixed port from `microservices.md`)
- `ServiceAccount` (IRSA annotation for AWS API access)
- `HorizontalPodAutoscaler` (CPU-based)
- `PodDisruptionBudget` (minAvailable)
- plus shared `NetworkPolicy` and `Ingress`.

## Step 1 — Prepare the ConfigMap and Secret

The chart references (does not own) these two objects:

### ConfigMap `integrity-config`

```bash
kubectl -n integrity create configmap integrity-config \
  --from-file=infra/config/application-kubernetes.yml   # or application-<env>.yml for MSK/RDS
```

**Why a ConfigMap?** It carries the non-secret runtime config (`platform.storage.endpoint`,
Kafka bootstrap, datasource URLs, …) for the environment's profile. The deployment template mounts
it read-only and the checksum annotation triggers a rollout when it changes.

### Secret `integrity-secrets`

The deploy pipeline builds it from GitHub secrets / Secrets Manager. Manually (first bootstrap):

```bash
kubectl -n integrity create secret generic integrity-secrets \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=RDS_PASSWORD="$RDS_PASSWORD" \
  --from-literal=KAFKA_SCRAM_USER="$KAFKA_SCRAM_USER" \
  --from-literal=KAFKA_SCRAM_PASSWORD="$KAFKA_SCRAM_PASSWORD" \
  --from-literal=REDIS_PASSWORD="$REDIS_PASSWORD" \
  --from-literal=MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY" \
  --from-literal=MINIO_SECRET_KEY="$MINIO_SECRET_KEY"
```

(Set those shell variables from the Secrets Manager entries created in step 08 — never type real
values into the command line where they can leak into shell history; prefer piping from
`aws secretsmanager get-secret-value`.)

## Step 2 — Deploy

```bash
helm upgrade --install integrity \
  infra/helm/interview-integrity \
  --namespace integrity \
  -f infra/helm/interview-integrity/values-dev.yaml \
  --set global.environment=dev
```

**What this does:**

- `--install` creates the release if it does not exist (first run).
- `-f values-dev.yaml` selects the environment's image registry, sizes, hosts, TLS secret names.
- The chart renders ~98 resources; Helm applies them in dependency order.

Expected output: `Release "integrity" has been upgraded. Happy Helming!`

## Step 3 — Watch the rollout

```bash
kubectl -n integrity get pods -w
kubectl -n integrity rollout status deployment --all
```

All deployments should reach `1/1 Ready` (or their replica count). The discovery service must be
ready first; other services re-register as they boot.

## Expected output

```text
NAME                                      READY   STATUS    RESTARTS
discovery-service-xxx                     1/1     Running   0
api-gateway-xxx                           1/1     Running   0
identity-service-xxx                      1/1     Running   0
... (19 deployments)
```

## Verification steps

1. All deployments `Ready`:
   ```bash
   kubectl -n integrity get deploy | grep -c '1/1'
   # 19
   ```
2. Registry shows all services registered:
   ```bash
   kubectl -n integrity port-forward svc/discovery-service 8761:8761 &
   open http://localhost:8761
   # 19 applications listed
   ```
3. Gateway health through a port-forward:
   ```bash
   kubectl -n integrity port-forward svc/api-gateway 8080:8080 &
   curl -s http://localhost:8080/actuator/health | jq .status
   # "UP"
   ```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `ImagePullBackOff` | ECR image missing or node can't pull | `kubectl describe pod <pod>`; confirm tag exists in ECR; check node role's ECR policy |
| `CrashLoopBackOff` | Config mismatch (DB/Kafka unreachable) | `kubectl logs <pod> --tail=50`; verify ConfigMap/Secret keys match what the profile expects |
| `CreateContainerConfigError` | Secret key missing | Compare `--from-literal` keys with `envFrom` names in `deployment.yaml` |
| `Invalid value: "..." port` | Values file port mismatch | Values ports must match `microservices.md` |
| HPA `FailedGetResourceMetric` | Metrics API not installed | Install the metrics-server (see `kubernetes.md` → HPA) |
| `configmap "integrity-config" not found` | Step 1 skipped | Create the ConfigMap, then `helm upgrade` again |

## Rollback procedure

```bash
# One-step rollback to the previous Helm release revision
helm rollback integrity <previous-revision> --namespace integrity --reuse-values
kubectl -n integrity rollout status deployment --all
```

`helm history integrity --namespace integrity` lists revisions so you can pick the right one.
The `deploy.yml` pipeline does exactly this automatically when its smoke test fails.

## Best practices

- Never edit `values.yaml` for an environment — change `values-<env>.yaml` (or the profile config)
  and re-run `helm upgrade`.
- Do routine deploys via `deploy.yml`; manual `helm` is for bootstrapping and emergencies.
- Keep `--reuse-values` on rollbacks so config isn't accidentally reset.
- After any ConfigMap/Secret change, rely on the checksum annotation to trigger the rollout
  automatically (it re-deploys when the checksum changes).

## Security notes

- Secrets are injected via `integrity-secrets`, never stored in values or image layers.
- The chart's NetworkPolicy is default-deny (see `infra/helm/interview-integrity/templates/networkpolicy.yaml`); if you relax it, you must understand what you are opening.
- Runbooks: `runbooks/pod-crash.md`, `runbooks/service-restart.md`, `runbooks/service-rollback.md`.
