# 08 — Provision the AWS Infrastructure

**Purpose.** To create the entire AWS footprint for an environment — VPC, EKS cluster, RDS,
MSK, ElastiCache, S3 buckets, Secrets Manager entries, IAM roles, ACM certificates — with one
Terraform command per environment. After this step your `dev`, `qa`, `uat`, and `prod` accounts
contain real, running infrastructure.

## Prerequisites

- Steps 01–07 completed (state bucket `integrity-terraform-state` exists).
- AWS CLI authenticated as `integrity-deployer`.
- Terraform 1.9.x.
- Git clone of the repository.

## Estimated Time

- `account` root (OIDC): 5 minutes.
- `dev`: 25–40 minutes (EKS node groups are slow).
- `qa`/`uat`/`prod`: 40–60 minutes each (RDS, MSK, multi-AZ).

## Required AWS permissions

Full `AdministratorAccess` (your `platform-admins` group) is needed here because the root creates
IAM roles, KMS keys, VPCs, EKS, RDS, MSK, ElastiCache, ACM, Route 53, and Secrets Manager. Later
you can reduce this (see step 20).

## What you are creating (per environment root)

| Module | Resources |
|---|---|
| `vpc` + `networking` | VPC, public/private subnets (3 AZs), NAT gateways, internet gateway, flow logs, security groups |
| `eks` | EKS control plane, managed node group(s), cluster autoscaler IAM |
| `iam` | Node/control-plane roles, IRSA roles, GitHub OIDC roles (account root) |
| `kms` | Per-service KMS keys (EBS, S3, RDS, MSK, secrets) |
| `rds` | PostgreSQL instance, subnet group, parameter group, master secret |
| `kafka` | MSK cluster (qa/uat/prod) or nothing for dev (Strimzi used instead) |
| `redis` | ElastiCache Redis cluster + auth secret |
| `s3` | `documents`, `reports`, `uploads` buckets + versioning/encryption |
| `secrets-manager` + `parameter-store` | JWT key, DB passwords, SCRAM credentials, config params |
| `ecr` | Repositories `integrity-<env>/<service>` for all 19 services |
| `alb` + `acm` + `route53` | ALB target group wiring, certificates, hosted zone records (prod) |
| `cloudwatch` + `monitoring` | Log groups, alarms, optional Prometheus/Grafana |

## Step A — Account-scoped OIDC (run once, for the whole account)

The GitHub Actions workflows must authenticate to AWS without storing keys. This root creates the
GitHub OIDC identity provider and (via the `iam` module in each environment) the roles the
pipelines assume.

```bash
cd terraform/account

# Download the AWS provider
terraform init

# Preview the OIDC provider creation
terraform plan \
  -var="github_organisation=sandipsmahajan" \
  -var="github_repository=Interview-Integrity-Platform"

# Apply (one time for the account)
terraform apply \
  -var="github_organisation=sandipsmahajan" \
  -var="github_repository=Interview-Integrity-Platform" \
  -auto-approve
```

**What this does:** creates `aws_iam_openid_connect_provider` for
`token.actions.githubusercontent.com`. Every environment's `iam` module imports it with a data
source, then creates roles like `integrity-<env>-github-actions`.

**Verify:**

```bash
aws iam list-open-id-connect-providers
# contains arn:aws:iam::<account>:oidc-provider/token.actions.githubusercontent.com
```

> **Only one OIDC provider per account.** This is why it lives in its own root: it must exist
> before any environment applies.

## Step B — Provision an environment (dev first)

### 1. Enter the environment root

```bash
cd terraform/environments/dev
```

### 2. Provide variables

The root has sensible defaults for `dev` (2–6 `m6i.large` nodes, `db.t4g.medium`, Strimzi for
Kafka). Overrides live in a local `terraform.tfvars` file (never committed):

```bash
cp ../../terraform.tfvars.example terraform.tfvars
# edit to taste (environment = "dev" is already set in the example)
```

Alternatively pass them as environment variables (the way CI does):

```bash
export TF_VAR_environment=dev
export TF_VAR_github_organisation=sandipsmahajan
export TF_VAR_github_repository=Interview-Integrity-Platform
```

**Why the CI/CD vars?** The `iam` module builds the GitHub OIDC role ARN from
`github_organisation` + `github_repository`, so these two variables are required on every
environment root.

### 3. Initialize with the remote backend

```bash
terraform init
```

**What this does:** downloads providers and configures the S3 backend created in step 07. You
should see `Initializing the backend... Successfully configured the backend "s3"!` and
`Terraform has been successfully initialized!`.

### 4. Plan

```bash
terraform plan -out=dev.tfplan
```

**What this does:** computes the entire desired infrastructure. Because this is the first run,
you will see a very large plan (200+ resources). Save it with `-out` so `apply` uses the exact
plan you reviewed.

> **Review the plan.** Look specifically for: correct VPC CIDR, expected node sizes, `db_multi_az
> = false`, `kafka_enabled = false` for dev, and the ECR repositories for all 19 services.

### 5. Apply

```bash
terraform apply dev.tfplan
```

**What this does:** creates everything. Watch for a long series of `Creating...`/`Still
creating...` lines — EKS node groups and RDS take several minutes each. When it finishes you will
see `Apply complete! Resources: <n> added, 0 changed, 0 destroyed.` followed by the outputs.

## Expected output (dev)

```text
Apply complete! Resources: 240 added, 0 changed, 0 destroyed.

Outputs:

api_url            = "https://api.dev.integritypro.example.com"
db_endpoint        = "integrity-dev.xxxx.us-east-1.rds.amazonaws.com"
db_master_secret_arn = "arn:aws:secretsmanager:us-east-1:...:secret:integrity/dev/rds-master-..."
eks_cluster_name   = "integrity-dev-eks"
kafka_bootstrap_servers = "kafka-kafka-bootstrap.kafka.svc.cluster.local:9092"
portal_url         = "https://portal.dev.integritypro.example.com"
redis_endpoint     = "integrity-dev.xxxx.cache.amazonaws.com"
```

## Verification steps

### 1. The EKS cluster is reachable

```bash
# Point kubectl at the new cluster (uses your CLI credentials via EKS auth)
aws eks update-kubeconfig --name integrity-dev-eks --region us-east-1

# Check the cluster is healthy
kubectl cluster-info
kubectl get nodes
```

`kubectl get nodes` must list the managed node group instances with `Ready`.

### 2. Data plane exists

```bash
# RDS instance status
aws rds describe-db-instances --query 'DBInstances[].{DB:DBInstanceIdentifier,Status:DBInstanceStatus,Class:DBInstanceClass}' --output table

# ElastiCache cluster
aws elasticache describe-cache-clusters --query 'CacheClusters[].{C:CacheClusterId,Status:CacheClusterStatus}' --output table

# S3 buckets
aws s3 ls | grep integrity
```

All must show `available`/`available`/the three buckets.

### 3. ECR repositories exist

```bash
aws ecr describe-repositories --query 'repositories[].repositoryName' --output table | grep integrity-dev
```

You should see `integrity-dev/api-gateway`, `integrity-dev/identity-service`, … (19 total).

### 4. Secrets exist

```bash
aws secretsmanager list-secrets --query 'SecretList[].Name' --output text
```

Contains the JWT key and RDS master secret for `dev`.

### 5. Outputs are captured

Save the output block (or `terraform output -json > outputs.json`) — steps 10–16 need the
endpoints, cluster name, and secret ARNs.

## Repeat for qa/uat/prod

```bash
# e.g. for prod — the variables.tf defaults differ (6-15 nodes, multi-AZ, WAF, rotation)
cd ../prod
cp ../../terraform.tfvars.example terraform.tfvars   # set environment=prod, sizing, kafka_enabled=true, ...
terraform init
terraform plan -out=prod.tfplan
terraform apply prod.tfplan
```

Key differences per environment (see `architecture/infrastructure.md` for the table):

- **prod**: `db_multi_az = true`, `db_enable_rotation = true`, `enable_waf = true`,
  `kafka_enabled = true`, 6–15 nodes.
- **qa**: `kafka_enabled = true` (MSK), single-AZ RDS.
- **uat**: like prod minus WAF/rotation, 3–12 nodes.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `Backend initialization required` | Ran `plan` before `init` | `terraform init` |
| `Error: creating EKS cluster ... AuthorizationError` | IAM propagation | Wait 30–60 s and `terraform apply` again (apply is idempotent) |
| `ServiceUnavailable` for node groups | AZ capacity | Re-run apply; choose different AZs in tfvars |
| `Error: acquiring state lock` | Another apply in progress (or stale lock) | Wait for it; if stale, delete the DynamoDB lock item (see `terraform.md`) |
| `KMS key not found` in node group | Key created in same apply not yet propagated | Re-run apply |
| Billing alarm fires during apply | EKS/RDS/MSK are expensive | That is normal during creation; it settles after autoscaling down |

## Rollback procedure

To **remove** an environment's infrastructure (e.g. a failed dev):

```bash
cd terraform/environments/dev
terraform destroy -auto-approve
```

**What this does:** deletes every resource in the reverse order of creation. `destroy` honors the
safety flags: RDS deletion protection and S3 `force_destroy = false` will make destroy **fail**
if they were enabled — that is deliberate. To decommission fully you must first disable deletion
protection in the console for that instance/bucket, then re-run `destroy`.

> Never run `terraform destroy` on a shared root while others are using it, and never destroy
> `prod` without a backup + restore test (see `disaster-recovery.md`).

## Best practices

- Apply `dev` first, verify, then qa → uat → prod. Never "test in prod".
- Use `-out=<env>.tfplan` + review the saved plan, or rely on the `terraform.yml` workflow's
  plan/apply for routine changes.
- Tag everything: the root's `common_tags` (owner, cost-center, env) propagate to all resources
  and make cost analysis trivial.
- Schedule `dev`/`qa` clusters to scale down outside working hours if budgets matter.

## Security notes

- The EKS cluster is **private**: nodes live in private subnets, the control plane is accessible
  via the VPC (and your CLI through EKS's public endpoint with IP allow-listing, or a VPC
  endpoint).
- All data-plane resources are encrypted at rest via the per-environment KMS keys.
- The GitHub OIDC roles created here are scoped per environment (`integrity-<env>-github-actions`)
  so a compromise of CI cannot touch another environment's infrastructure.
- Never commit `terraform.tfvars`; it may contain DNS, alarm email, and sizing choices you do not
  want public.
