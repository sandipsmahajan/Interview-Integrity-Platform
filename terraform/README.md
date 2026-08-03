# Integrity Pro — Infrastructure as Code

AWS infrastructure for the Integrity Pro platform, written in Terraform and
applied by GitHub Actions using OIDC (no static credentials). The same modules
provision all five environments; each environment is an isolated remote-state
root.

## Repository Layout

```
terraform/
  bootstrap/               # state bucket + DynamoDB lock table + KMS key (run once)
  account/                 # GitHub OIDC provider (run once, account-scoped)
  environments/
    local/  dev/  qa/  uat/  prod/    # isolated roots; backend per environment
  modules/
    shared          # naming convention + tags
    security-groups # security-group factory
    kms             # per-use KMS keys (rds/redis/msk/s3/ecr/eks/secrets/logs)
    iam             # GitHub OIDC provider + CI/CD role + EKS access
    vpc             # VPC, subnets, NAT, NACLs, flow logs
    networking      # VPC endpoints (S3/DynamoDB gateway + interface endpoints)
    s3              # object-storage + log buckets (versioning/lifecycle/KMS)
    ecr             # container registries for the 19 services
    rds             # PostgreSQL 16.4, multi-AZ, PITR, Secrets Manager, rotation
    redis           # ElastiCache Redis with TLS/at-rest encryption + auth
    kafka           # Amazon MSK (SASL/SCRAM); disabled -> Strimzi in-cluster
    secrets-manager # secret + password-rotation Lambda
    parameter-store # SSM parameters
    eks             # control plane, managed node group, addons, IRSA, access
    alb             # internet-facing ALB -> nginx ingress NodePort 30080
    acm             # certificates + Route53 DNS validation
    route53         # hosted zone + api/portal aliases
    cloudwatch      # log groups + ERROR metric filters
    monitoring      # SNS + alarms + overview dashboard
```

## One-Time Bootstrap (CLI only, no AWS Console)

Run these once per AWS account. Everything else is managed by Terraform.

### 1. State backend

```bash
cd terraform/bootstrap
terraform init
terraform apply -var="state_bucket_name=integrity-terraform-state"
```

Creates `integrity-terraform-state` (S3, versioned, KMS-encrypted) and
`integrity-terraform-locks` (DynamoDB).

### 2. GitHub OIDC provider

```bash
cd terraform/account
terraform init
terraform apply
```

Creates the OIDC provider `token.actions.githubusercontent.com` (account-scoped).
All environments reference it via a data source — only this root creates it.

### 3. Environment roots

For each environment (local/dev/qa/uat/prod):

```bash
cd terraform/environments/dev
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Each root also creates its own `integrity-<env>-github-actions` role with the
OIDC trust policy, so the GitHub workflows can assume it.

## CI/CD Integration

| Workflow | Trigger | Action |
| --- | --- | --- |
| `terraform` plan | PR touching `terraform/**` | validate all roots + plan all envs |
| `terraform` apply | push to `master` | apply `dev` |
| `terraform` apply | `workflow_dispatch` | apply chosen env (qa/uat/prod gated by GitHub environments) |

The workflows assume the per-env role:

```
arn:aws:iam::<ACCOUNT_ID>:role/integrity-<env>-github-actions
```

Required GitHub configuration:

- Repository **variable** `AWS_ACCOUNT_ID`.
- GitHub **environments** `dev`, `qa`, `uat`, `prod` (approval reviewers for
  qa/uat/prod).
- Environment **secrets**: `JWT_SECRET_DEV`, `JWT_SECRET_QA`,
  `JWT_SECRET_UAT`, `JWT_SECRET_PROD` (used as `TF_VAR_jwt_secret`).

## Making Changes

1. Edit a module in `terraform/modules/<name>/`.
2. Validate every root:

   ```bash
   terraform fmt -check -recursive terraform/
   for root in terraform terraform/environments/{local,dev,qa,uat,prod}; do
     (cd "$root" && terraform init -backend=false && terraform validate)
   done
   ```

3. Commit; the PR pipeline plans all environments. Apply via merge or
   `workflow_dispatch`.

## Security Notes

- **No secrets in tfvars.** `TF_VAR_jwt_secret` and CI-provided variables
  supply sensitive values; `random_password` resources generate the rest.
- State is encrypted at rest (KMS) and in transit (TLS).
- The CI/CD role is scoped to the state bucket/lock table, ECR, EKS access
  entries, Secrets Manager/SSM for the environment prefix, and Route53.
- RDS/ElastiCache/MSK security groups only allow ingress from the EKS node SG.
