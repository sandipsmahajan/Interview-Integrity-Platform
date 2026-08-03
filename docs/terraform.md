# Terraform Guide

**Purpose.** The complete reference for working with the Terraform codebase that owns Integrity
Pro's AWS infrastructure: how it is structured, how state works, and how to plan, apply, destroy,
import, upgrade, and migrate safely.

> The *deployment* story (first bootstrap) is in `deployment/07-*` and `deployment/08-*`. This
> document is the *ongoing* reference for engineers who will live in `terraform/`.

---

## 1. Terraform structure

```text
terraform/
├── bootstrap/                  # one-time: S3 state bucket + DynamoDB lock table (LOCAL backend)
├── account/                    # one-time: GitHub OIDC provider (account-scoped)
├── environments/
│   ├── local/  dev/  qa/  uat/  prod/
│   │   ├── main.tf             # wires modules together
│   │   ├── providers.tf        # aws + kubernetes + helm providers
│   │   ├── backend.tf          # remote state (S3 + DynamoDB lock)
│   │   ├── variables.tf        # input variables with sensible defaults
│   │   ├── locals.tf           # derived values (names, tags)
│   │   ├── outputs.tf          # cluster name, endpoints, secret ARNs
│   │   └── versions.tf         # terraform + provider version pins
├── modules/
│   ├── shared/  iam/  kms/  vpc/  networking/  s3/  ecr/
│   ├── rds/  redis/  kafka/  secrets-manager/  parameter-store/
│   ├── eks/  alb/  acm/  route53/  cloudwatch/  monitoring/
└── terraform.tfvars.example    # documented example inputs
```

**Why this layout?**

- Each environment is its **own root** with its **own state file**. A mistake in `prod` cannot
  corrupt `dev`.
- Modules are the shared building blocks; environments are thin compositions.
- `bootstrap` and `account` run once and are deliberately separate because they cannot depend on
  the infrastructure they create.

## 2. Modules

A module is a reusable unit (a folder with `variables.tf`, `main.tf`, `outputs.tf`). The
environment roots call modules like functions:

```hcl
module "eks" {
  source = "../../modules/eks"
  cluster_name        = local.cluster_name
  cluster_version     = var.eks_cluster_version
  vpc_id              = module.vpc.vpc_id
  node_desired_size   = var.eks_node_desired_size
  common_tags         = local.common_tags
}
```

| Module | Owns | Notable inputs |
|---|---|---|
| `shared` | Tags, naming, common locals | `project`, `environment` |
| `iam` | GitHub OIDC roles, node/control-plane roles, IRSA roles | `github_organisation`, `github_repository`, `project` |
| `kms` | Per-service KMS keys + aliases | service list |
| `vpc` | VPC, subnets, NAT, IGW, flow logs | CIDRs, AZs |
| `networking` | Security groups | app/data CIDRs |
| `s3` | State bucket, `documents`/`reports`/`uploads` | versioning, encryption |
| `ecr` | 19 image repositories | service list, lifecycle |
| `rds` | PostgreSQL, subnet group, master secret | instance class, multi-AZ, backup retention |
| `redis` | ElastiCache cluster + auth secret | node type, cluster count |
| `kafka` | MSK cluster + SCRAM secret + topics | instance type, broker count |
| `secrets-manager` | JWT/DB/SCRAM secrets + rotation | rotation Lambda |
| `parameter-store` | Non-secret config params | key/value map |
| `eks` | Control plane, node groups, kubeconfig | cluster version, node sizing |
| `alb` | Load balancer + target groups + listeners | nodePort, cert ARN |
| `acm` | Certificates + DNS validation | domain |
| `route53` | Hosted zone + records | domain, alb DNS |
| `cloudwatch` | Log groups + alarms | retention, alarm email |
| `monitoring` | Optional Prometheus/Grafana | — |

**Rule:** modules never hard-code environment-specific values; they receive them as variables.
This is what lets all five environments share the same modules.

## 3. Remote state

State is Terraform's memory of what exists. It must be shared and safe.

```hcl
# terraform/environments/dev/backend.tf
terraform {
  backend "s3" {
    bucket         = "integrity-terraform-state"
    key            = "integrity/dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "integrity-terraform-locks"
    encrypt        = true
  }
}
```

| Key | Meaning |
|---|---|
| `bucket` | The S3 bucket created in `bootstrap` |
| `key` | Unique state object per environment (`integrity/<env>/terraform.tfstate`) |
| `region` | Where the bucket lives |
| `dynamodb_table` | Lock table (created in `bootstrap`) |
| `encrypt` | SSE at rest on the state object |

**Rules of state:**

- State never lives locally for environment roots. `backend.tf` is the single source.
- State may contain secrets → bucket is versioned + KMS-encrypted + private.
- Never edit `.tfstate` by hand. Use `terraform` commands only.

## 4. State locking

Before `apply`, Terraform takes a lock in DynamoDB:

```hcl
resource "aws_dynamodb_table" "terraform_locks" {
  name         = "integrity-terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  attribute {
    name = "LockID"
    type = "S"
  }
}
```

**What locking prevents:** two engineers running `terraform apply` on the same environment at the
same time. The second one fails with:

```text
Error: Error acquiring the state lock
```

**Handling a stale lock** (lock held by a dead process / crash):

```bash
cd terraform/environments/<env>
terraform force-unlock <lock-id>   # the <lock-id> is printed in the error
```

Only force-unlock if you are **certain** no other apply is running — otherwise you can corrupt
state.

## 5. Variables

| Mechanism | Precedence | Used by |
|---|---|---|
| `-var="key=value"` | highest | manual CLI |
| `TF_VAR_key` env var | | CI (secrets stay out of CLI history) |
| `terraform.tfvars` / `.auto.tfvars` | | local overrides (never committed) |
| defaults in `variables.tf` | lowest | safe fallbacks |

**Never** put secrets in `terraform.tfvars`. Secrets flow in as `TF_VAR_*` from the pipeline or
are fetched at runtime from Secrets Manager.

Example:

```bash
# local override file (git-ignored)
cd terraform/environments/dev
cat > terraform.tfvars <<'EOF'
environment            = "dev"
eks_node_desired_size  = 3
alarm_email            = "ops@example.com"
EOF

# or as env vars (CI style)
export TF_VAR_github_organisation=sandipsmahajan
export TF_VAR_github_repository=Interview-Integrity-Platform
```

## 6. Outputs

Outputs surface the facts other tools need. All env roots export:

```bash
cd terraform/environments/dev
terraform output          # all outputs
terraform output -json    # machine-readable (used by the pipeline)
terraform output eks_cluster_name
```

Common outputs: `eks_cluster_name`, `db_endpoint`, `db_master_secret_arn`,
`redis_endpoint`, `redis_auth_secret_arn`, `kafka_bootstrap_servers`, `api_url`, `portal_url`.

## 7. Environments

| Root | State key | Notes |
|---|---|---|
| `local` | — | Reference config for laptops; not applied |
| `dev` | `integrity/dev/terraform.tfstate` | Small, in-cluster data plane |
| `qa` | `integrity/qa/...` | MSK + RDS |
| `uat` | `integrity/uat/...` | Multi-AZ |
| `prod` | `integrity/prod/...` | Multi-AZ + WAF + rotation |

Always work inside the environment's root and **double-check the directory** before applying:

```bash
pwd   # must end in terraform/environments/<env>
```

## 8. Plan and apply

```bash
cd terraform/environments/<env>
terraform init                # download providers + configure backend (first time / after changes)
terraform plan -out=env.tfplan  # compute diff; write it to a file
terraform apply env.tfplan     # apply exactly the reviewed plan
```

**The discipline:**

1. `terraform plan` and **read it**. Check for unexpected `-` (deletes).
2. Save with `-out=` so apply cannot drift from the reviewed plan.
3. Prefer the pipeline (`terraform.yml`) for shared environments; it plans in the PR and applies
   only on approval.

Shortcuts that are *not* recommended but valid:

```bash
terraform apply -auto-approve        # no "yes" prompt (pipeline uses this)
terraform apply -target=module.xxx   # apply only one module (use sparingly)
```

## 9. Destroy

```bash
cd terraform/environments/<env>
terraform destroy -auto-approve
```

**Safety rails in this codebase:**

- RDS `deletion_protection = true` (prod) → `destroy` **fails** on RDS. Deliberate.
- S3 buckets have `force_destroy = false` → `destroy` fails on non-empty buckets. Deliberate.
- To actually decommission, you must first disable those protections explicitly, in the console,
  with a change ticket — this is the "two-person rule" in action.

**Never destroy:**
- the `bootstrap` bucket while any environment state exists in it;
- an environment others depend on;
- `prod` without a completed DR restore drill.

## 10. Import

Bring existing resources under Terraform management (e.g. resources created by hand):

```bash
cd terraform/environments/<env>
terraform import module.s3.aws_s3_bucket.state integrity-terraform-state
```

**Workflow:**

1. Add the resource block to the module/root (matching real attributes).
2. Run `terraform import <address> <resource-id>`.
3. Run `terraform plan` — it should show **no** diff for the imported resource.
4. Apply if anything needs reconciling.

Import does not modify the resource; it only records it in state. Use it rarely — the platform's
goal is that *nothing* is created by hand.

## 11. Upgrade

### Upgrading Terraform itself

```bash
# Read the constraint first
grep required_version terraform/environments/*/versions.tf | sort -u
# "~> 1.9.0" -> any 1.9.x is fine; going 1.9 -> 2.x is a major change
```

Install the new version, then **re-init** each root:

```bash
cd terraform/environments/<env>
terraform init -upgrade   # refresh provider/lockfile against the new version
terraform plan            # confirm no surprising diffs
```

### Upgrading a provider (e.g. AWS)

```bash
terraform init -upgrade
terraform plan            # review the diff carefully
terraform apply
```

Provider upgrades can reformat resources (or worse). Sequence them one environment at a time,
`dev` → `prod`, with a plan review each step.

### Upgrading EKS (control plane / node groups)

Change `eks_cluster_version` in `variables.tf`, plan, apply. EKS handles the control plane
upgrade; node groups are rolled by EKS. Kubernetes minor upgrades may require client/tooling
updates — see `kubernetes.md` → Upgrading.

## 12. Migration

### State migration between backends

If the state bucket or key ever changes:

```bash
# update backend.tf, then
cd terraform/environments/<env>
terraform init -migrate-state
```

`-migrate-state` copies existing state to the new backend **without losing it**. Confirm the
copy with `terraform state list` (should show the same addresses as before).

### Moving a resource to a new address (refactor)

```bash
terraform state mv module.old.path resource new.path resource
```

Use it after renaming a module/resource so state stays in sync without an apply.

### Adopting a new environment

Copy an existing root (e.g. `qa` → `prod`), edit `variables.tf` defaults + `terraform.tfvars`,
set a unique state `key`, then `init && plan && apply`.

## 13. Troubleshooting Terraform

| Symptom | Cause | Fix |
|---|---|---|
| `Error acquiring the state lock` | Concurrent apply / stale lock | Wait; if stale, `terraform force-unlock <id>` |
| `Backend initialization required` | `init` skipped | `terraform init` |
| `Invalid legacy provider address` | Old lockfile | `terraform init -upgrade` |
| `Error: creating EKS cluster: AuthorizationError` | IAM not propagated | Wait 30–60 s, re-apply |
| `Plan shows unexpected deletions` | A variable changed the module wiring | Revert the variable; use `terraform state mv` if a refactor |
| `Provider produced inconsistent final plan` | Provider bug / drift | `terraform init -upgrade`, re-plan; open issue if persistent |
| KMS errors on node group | Key propagation | Re-apply; ensure key policy includes the node role |
| `terraform validate` fails in CI | Stale lockfile / wrong version | Use `terraform init -backend=false` for validate-only jobs |

## 14. Terraform in CI/CD

`.github/workflows/terraform.yml`:

1. **PR**: `terraform fmt -check`, `terraform init -backend=false`, `terraform validate` on every
   root; then `terraform plan` per environment using the OIDC role
   `integrity-<env>-github-actions` and posts the plan as a PR comment.
2. **Merge/approve**: `terraform apply` on the environment whose GitHub environment approval
   passed.

The pipeline never runs `destroy` automatically. Destruction is a deliberate, reviewed, manual
act.

## 15. Best practices (summarized)

- `terraform fmt -recursive` before every commit.
- Plan in the PR, apply on approval — never `apply` without a reviewed plan.
- One environment at a time; `dev` → `qa` → `uat` → `prod`.
- Keep `tfvars` and state out of Git.
- Prefer `TF_VAR_*` for secrets; `tfvars` for non-secrets.
- Back up state by backing up the versioned S3 bucket (it's already versioned).
- Treat the lock table like a critical control — never delete it.

## Security notes

- State is KMS-encrypted and versioned; restrict bucket/table access to `platform-admins` and the
  OIDC roles.
- `terraform plan` can leak values into logs/PRs if you `-no-color` on secrets — the pipeline
  masks `TF_VAR_*` secret values.
- Never store AWS keys in `.tf` files; the AWS provider reads your CLI profile or OIDC role.
