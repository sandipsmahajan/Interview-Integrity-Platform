# 11 — Install the Strimzi Kafka Operator (dev / local only)

**Purpose.** To install the Strimzi Kafka operator, which runs in-cluster Kafka for the `dev` and
`local` data plane. **Skip this step for `qa`/`uat`/`prod`** — they use Amazon MSK, which is
provisioned by Terraform (step 08) with no operator needed.

## Prerequisites

- Step 08 completed (EKS cluster `integrity-dev-eks` reachable via `kubectl`).
- Helm installed (step 06).

## Estimated Time

15 minutes.

## Required AWS permissions

None beyond your `kubectl` access to the cluster. If you reach `kubectl` via the AWS CLI, your
IAM user needs the `EKS:AccessKubernetesApi` permission (included in `AdministratorAccess`).

## What Strimzi is and why dev uses it

Strimzi is a Kubernetes operator: it watches custom resources (`Kafka`, `KafkaTopic`) and
manages brokers, topics, and access for you. Using it in `dev` gives developers a **real Kafka**
with the exact same topic topology as production, at zero AWS cost. Production uses Amazon MSK so
that the broker is fully managed — see [`kafka.md`](../architecture/kafka.md).

## Steps

### 1. Create the `kafka` namespace

The operator is isolated in its own namespace (matching `infra/k8s/namespace.yaml` and the
strimzi install command in the manifest header).

```bash
kubectl create namespace kafka
kubectl create namespace integrity   # if not already created by the platform manifests
```

### 2. Add the Strimzi Helm repo

```bash
helm repo add strimzi https://strimzi.io/charts/
helm repo update
```

**What this does:** registers Strimzi's chart repository (already added in step 06) and refreshes
the index.

### 3. Install the operator

```bash
helm install strimzi-operator strimzi/strimzi-kafka-operator -n kafka \
  --set watchAnyNamespace=false \
  --set watchNamespaces={kafka}
```

**What this does:**

- Installs the operator deployment and its CRDs (`Kafka`, `KafkaTopic`, `KafkaUser`, …).
- `watchNamespaces={kafka}` limits it to managing Kafka resources **only** in the `kafka`
  namespace — a least-privilege choice: the operator can never touch `integrity` workloads.

Expected output: `NAME: strimzi-operator ... STATUS: deployed`.

### 4. Wait for the operator

```bash
kubectl -n kafka rollout status deploy/strimzi-cluster-operator
```

## Expected output

```text
deployment "strimzi-cluster-operator" successfully rolled out
```

## Verification steps

```bash
# The operator is running
kubectl -n kafka get pods
# strimzi-cluster-operator-xxxx-xxxx    1/1     Running

# The CRDs are registered
kubectl get crd | grep strimzi
# kafkas.kafka.strimzi.io, kafkatopics.kafka.strimzi.io, kafkausers.kafka.strimzi.io, ...
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `Error: could not find a ready tiller` | Helm 2 | You are on Helm 2; use Helm 3 |
| `chart requires namespace kafka` | Namespace missing | `kubectl create namespace kafka` then retry |
| Operator crashes with `error creating clientset` | RBAC propagation | Wait a minute and check `kubectl -n kafka get events`; re-apply if the operator's ServiceAccount lacks permissions |
| `watchNamespaces` syntax error | Helm parsing braces in zsh | Quote the value: `--set "watchNamespaces={kafka}"` |

## Rollback procedure

```bash
# Uninstall the operator (leaves any Kafka cluster data behind until step 12's Kafka is removed)
helm uninstall strimzi-operator -n kafka
```

## Best practices

- Keep the operator's RBAC scoped to `watchNamespaces={kafka}`.
- Upgrade the operator deliberately (releases are rare), and only after testing in `dev`.
- Do **not** install the operator in environments that use MSK — it would be dead weight and a
  second broker manager.

## Security notes

- The operator needs high privileges to manage brokers, but only inside its namespace — this is
  exactly why it is installed into `kafka` with a narrow watch scope.
- The plaintext listener (`:9092`) is acceptable inside the private cluster for dev; `prod` uses
  SASL/SCRAM on MSK. Never expose the `kafka` namespace to the internet.
