# 04 — Install Terraform

**Purpose.** To install the exact Terraform version the repository is written for, so `terraform
plan` and `terraform apply` behave identically for everyone.

## Prerequisites

- Steps 01–03 completed.
- `curl` and `unzip` available (verify with `curl --version`, `unzip -v`).

## Estimated Time

10 minutes.

## Required AWS permissions

None for installation. Terraform's AWS provider uses the CLI credentials from step 03 when you run
plans (later steps).

## What Terraform is

Terraform is an Infrastructure-as-Code (IaC) tool: you declare AWS resources in `.tf` files, and
Terraform figures out the diffs and applies them. Everything about the platform's AWS footprint is
in `terraform/` — no click-ops.

## Version requirement

The repository pins a compatible range. Read it:

```bash
cat terraform/environments/dev/versions.tf
```

You will see something like:

```hcl
terraform {
  required_version = "~> 1.9.0"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}
```

`~> 1.9.0` means "any 1.9.x". Install a 1.9.x release.

## Steps

### 1. Install

**macOS (Homebrew):**

```bash
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

**Linux (official HashiCorp repo):**

```bash
wget -O- https://apt.releases.hashicorp.com/gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] \
  https://apt.releases.hashicorp.com $(lsb_release -cs) main" | \
  sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install -y terraform
```

**Direct binary (any Linux):**

```bash
# Check the latest 1.9.x tag at releases.hashicorp.com and adjust the URL
curl -LO https://releases.hashicorp.com/terraform/1.9.8/terraform_1.9.8_linux_amd64.zip
unzip terraform_1.9.8_linux_amd64.zip
sudo mv terraform /usr/local/bin/
```

### 2. Verify

```bash
terraform version
# Terraform v1.9.8
```

### 3. Confirm the repo validates

```bash
cd terraform/environments/dev
terraform init          # downloads providers (see note below)
terraform validate      # static checks of the config
```

**What `terraform init` does:** reads `providers.tf`, downloads the AWS/Kubernetes/Helm provider
plugins into `.terraform/`, and configures the state backend. It is required once per root and
again whenever providers change. It makes **no changes** to AWS.

> **Disk space:** provider downloads are ~100 MB per root. The repository's `.gitignore`
> excludes `.terraform/`. To save space across the five roots, set a shared plugin cache:
> `export TF_PLUGIN_CACHE_DIR="$HOME/.terraform.d/plugin-cache"` (persist it in your shell rc).

## Expected output

- `terraform version` prints 1.9.x.
- `terraform validate` in any environment root prints `Success! The configuration is valid.`

## Verification steps

```bash
cd terraform/environments/dev
terraform fmt -check     # no output = formatting is clean
terraform validate       # "Success! The configuration is valid."
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `Terraform has been successfully initialized` but `validate` complains | Missing backend/creds | `terraform init -backend=false` for validate-only, or complete step 07 first |
| `Invalid provider version` | Wrong Terraform installed | Install 1.9.x (`terraform version`) |
| `The given credentials were not valid` | CLI not configured | Complete step 03 |
| `Failed to download provider` | Network/proxy | Check connectivity to registry.terraform.io; set `HTTPS_PROXY` |

## Rollback procedure

- Uninstall: `brew uninstall terraform` or remove `/usr/local/bin/terraform`. No AWS resources
  are affected by the tool itself — rollback is only about local tooling.

## Best practices

- **Pin the version** (this repo does via `required_version`). Never run `terraform plan` with an
  unreviewed newer major.
- Use the plugin cache so CI and your laptop share provider downloads.
- Run `terraform fmt` before every commit (`terraform fmt -recursive` at the repo root).

## Security notes

- `terraform init` downloads provider binaries; install only from `releases.hashicorp.com` and
  never from random mirrors.
- Credentials come from your CLI profile (step 03) — Terraform never stores AWS keys itself, but
  `.terraform/terraform.tfstate` may contain sensitive plan data if you run `terraform apply`
  before step 07's remote backend exists. Always apply after configuring remote state.
