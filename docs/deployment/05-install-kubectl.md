# 05 — Install kubectl

**Purpose.** To install `kubectl`, the command-line client for Kubernetes, which you will use to
inspect and manage the EKS cluster that Terraform creates.

## Prerequisites

- Steps 01–04 completed.
- `curl` available.

## Estimated Time

10 minutes.

## Required AWS permissions

None for installation. Using it against EKS requires the cluster access from step 08.

## What kubectl is

`kubectl` talks to the Kubernetes API server. It is the universal "admin console" for the
cluster: you use it to see pods, services, deployments, logs, and to roll back releases.

## Steps

### 1. Install

**macOS (Homebrew):**

```bash
brew install kubectl
```

**Linux (official binary):**

```bash
# Fetch the current stable client version
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

**Verify the checksum (optional but recommended):**

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
```

### 2. Verify

```bash
kubectl version --client
# Client Version: v1.3x.x
```

### 3. Enable shell completion (optional but pleasant)

```bash
echo 'source <(kubectl completion zsh)' >> ~/.zshrc   # zsh
echo 'source <(kubectl completion bash)' >> ~/.bashrc  # bash
```

## Expected output

- `kubectl version --client` prints a v1.3x client.

## Verification steps

You cannot reach a cluster yet (none exists). A clean way to confirm the client works is:

```bash
kubectl version --client -o yaml
# clientVersion is populated; serverVersion is absent (no cluster configured)
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `kubectl: command not found` | Not on PATH | `sudo mv kubectl /usr/local/bin/` and re-open the shell |
| `The connection to the server localhost:8080 was refused` | No `kubeconfig` / no cluster | Expected until step 08 configures `kubectl` for EKS |
| `error: no configuration has been provided` | `~/.kube/config` missing | Step 08 runs `aws eks update-kubeconfig` |

## Rollback procedure

- Uninstall: `brew uninstall kubectl` or remove `/usr/local/bin/kubectl`. No cluster state is
  affected.

## Best practices

- Keep `kubectl` within one minor version of the EKS Kubernetes version (the EKS control plane
  is set in `terraform/modules/eks`; check with `kubectl version` after step 08).
- Use `kubectl -n integrity` for all Integrity Pro commands — the namespace is your workbench.
- Never alias `kubectl` to bypass `-n`; forgetting the namespace is the most common kubectl
  mistake.

## Security notes

- kubectl authenticates to EKS through the AWS CLI's credentials (via `aws eks
  update-kubeconfig`). If your CLI key is compromised, your cluster access is compromised — keep
  the key from step 03 safe and rotate it regularly.
- Use short-lived sessions where possible (the CLI supports `aws configure sso` for that);
  at minimum, scope the CLI user's cluster access rather than giving it `cluster-admin`.
