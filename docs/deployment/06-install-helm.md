# 06 — Install Helm

**Purpose.** To install Helm, the package manager for Kubernetes, which deploys all Integrity Pro
workloads from the chart in `infra/helm/interview-integrity`.

## Prerequisites

- Steps 01–05 completed.

## Estimated Time

10 minutes.

## Required AWS permissions

None for installation. Helm talks to the cluster through your `kubeconfig` (step 08).

## What Helm is

Helm turns a *chart* (templates + values) into real Kubernetes resources. Integrity Pro uses one
umbrella chart; running `helm upgrade` renders all 19 deployments with the environment's values
file. Helm records every release as a **revision**, which is what makes `helm rollback` instant.

## Steps

### 1. Install

**macOS (Homebrew):**

```bash
brew install helm
```

**Linux (official script):**

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### 2. Verify

```bash
helm version
# version.BuildInfo{Version:"v3.1x.x", ...}
```

### 3. Add the repositories the chart needs (for future `helm repo update`)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add strimzi https://strimzi.io/charts/
helm repo update
```

> These repos are only needed when installing the operator/ingress stacks yourself. The Integrity
> Pro chart itself is **local** (`infra/helm/interview-integrity`) and needs no repo.

## Expected output

- `helm version` prints v3.1x.

## Verification steps

Validate the platform chart without touching any cluster:

```bash
helm lint infra/helm/interview-integrity
```

Expected: no errors, at most warnings. Then render the templates to prove the chart parses:

```bash
helm template dev infra/helm/interview-integrity \
  -f infra/helm/interview-integrity/values-dev.yaml \
  | wc -l
# hundreds of lines = many rendered resources
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `command not found: helm` | Not installed/on PATH | Re-install; re-open shell |
| `error: no configuration has been provided` | No cluster yet | Expected until step 08 |
| `lint: chart.yaml: validation` errors | Chart broken | Run `helm lint` on the chart; fix `Chart.yaml` |
| `Error: unknown flag: --reuse-values` | Helm v2 | You are on Helm 2; use Helm 3 (`helm version`) |

## Rollback procedure

- Uninstall: `brew uninstall helm` or remove the binary. Helm itself stores nothing outside your
  local machine; all release state lives in the cluster (`kube-system` namespace secrets).

## Best practices

- Always deploy with `-f values-<env>.yaml` — never edit `values.yaml` for an environment.
- Inspect what a release *would* change before applying: `helm diff upgrade` (the
  `helm-diff` plugin) or at minimum `helm template ... | kubectl diff -f -`.
- Keep the chart under review in Git like application code — config changes are code changes.

## Security notes

- Helm uses your `kubeconfig` identity; it does not store AWS credentials.
- Charts from third-party repos are third-party code — review them before installing. The
  Integrity Pro chart is first-party and reviewed in this repository.
