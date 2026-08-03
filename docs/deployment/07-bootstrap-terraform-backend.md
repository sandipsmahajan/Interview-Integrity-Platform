# 07 — Bootstrap the Terraform Backend

**Purpose.** To create the three pieces of shared state infrastructure every environment depends
on: an S3 bucket that stores Terraform state, a DynamoDB table that locks state during applies,
and a KMS key that encrypts the state at rest. This is a **one-time** step for the whole account.

## Prerequisites

- Steps 01–06 completed.
- AWS CLI authenticated as `integrity-deployer` (step 03).
- Terraform 1.9.x (step 04).
- An idea of your AWS **account ID** (`aws sts get-caller-identity` → `Account`).

## Estimated Time

10 minutes.

## Required AWS permissions

Your `platform-admins` user has `AdministratorAccess`, which covers S3, DynamoDB, and KMS
creation. If you later reduce this user's permissions, this step needs at least:

- `s3:CreateBucket`, `s3:PutBucketVersioning`, `s3:PutBucketEncryption`, `s3:PutBucketPublicAccessBlock`
- `dynamodb:CreateTable`
- `kms:CreateKey`, `kms:CreateAlias`

## What you are creating and why

| Resource | Name | Why |
|---|---|---|
| S3 bucket | `integrity-terraform-state` (see note below) | Stores each environment's `terraform.tfstate` under a distinct key like `integrity/dev/terraform.tfstate` |
| DynamoDB table | `integrity-terraform-locks` | Lets Terraform **lock** state during `apply`, so two people cannot corrupt it by applying simultaneously |
| KMS key + alias | `alias/integrity/terraform-state` | Encrypts state at rest (SSE-KMS) and the lock table |

> **Bucket-name note (important).** The bootstrap module's default bucket name is
> `integrity-terraform-state-<account-id>`, but every environment root's `backend.tf` references
> `integrity-terraform-state`. To keep them consistent, **pass the bucket name explicitly** exactly
> as shown below. If you ever change the bucket name, you must update all five `backend.tf` files
> to match (and re-run `terraform init -migrate-state`).

## Steps

### 1. Change into the bootstrap root

```bash
cd terraform/bootstrap
```

**Why this directory?** It is intentionally separate from the environment roots: it must create
the very state bucket that everything else uses, so it starts with the **local** backend
(`backend.tf` contains `backend "local" {}`).

### 2. Initialize

```bash
terraform init
```

**What this does:** downloads the AWS provider and prepares the local backend. Expected output ends
with `Terraform has been successfully initialized!`.

### 3. Preview what will be created

```bash
terraform plan -var="state_bucket_name=integrity-terraform-state"
```

**What this does:** calculates the diff between the current account and the desired state, without
changing anything. Expected output shows `Plan: 5 to add` (the KMS key, its alias, the bucket,
versioning/encryption configuration blocks, and the DynamoDB table).

> **Always plan before apply.** If the plan shows anything unexpected (e.g. deleting resources),
> stop and investigate.

### 4. Apply

```bash
terraform apply -var="state_bucket_name=integrity-terraform-state" -auto-approve
```

**What this does:** creates the resources. `-auto-approve` skips the interactive "yes" prompt;
omit it if you prefer to type `yes`. Expected output ends with `Apply complete! Resources: 5
added, 0 changed, 0 destroyed.`

## Expected output

```text
Apply complete! Resources: 5 added, 0 changed, 0 destroyed.

Outputs:
...
```

Verify from the console or CLI:

```bash
aws s3api get-bucket-versioning --bucket integrity-terraform-state
# {"Status": "Enabled"}

aws dynamodb describe-table --table-name integrity-terraform-locks --query 'Table.TableStatus' --output text
# ACTIVE
```

## Verification steps

1. `aws s3 ls s3://integrity-terraform-state` — the bucket exists and is empty.
2. The DynamoDB table is `ACTIVE` (command above).
3. Check the bucket is encrypted:
   ```bash
   aws s3api get-bucket-encryption --bucket integrity-terraform-state --query 'ServerSideEncryptionConfiguration'
   ```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `BucketAlreadyExists` | The bucket name is taken globally | Use your account-id suffix: `-var="state_bucket_name=integrity-terraform-state-<account-id>"` and update the five `backend.tf` files |
| `AccessDenied` on `s3:CreateBucket` | IAM user lacks permission | Re-check the permission list above; confirm `aws sts get-caller-identity` shows `integrity-deployer` |
| `Insufficient permissions` for KMS key policy | KMS key policy references root | The bootstrap key policy allows the account root; wait a minute for IAM propagation and retry |
| `failed to update S3 bucket versioning` | Bucket created without versioning from a previous partial run | Apply again; the module's `aws_s3_bucket_versioning` resource fixes it |

## Rollback procedure

To undo this step (only if you made a mistake and nothing else uses the state yet):

```bash
cd terraform/bootstrap
terraform destroy -var="state_bucket_name=integrity-terraform-state" -auto-approve
```

> This deletes the state bucket and lock table. **Only** safe before any environment root has
> written state. Afterwards, destroying the bucket would delete every environment's Terraform
> state — never do that. (The module guards the bucket with `force_destroy = false` for exactly
> this reason.)

## Best practices

- Keep this root's `terraform.tfvars` (with your chosen bucket name) in the repository reviewable
  state, but never commit real credentials.
- Record the KMS key ARN and DynamoDB table name in the team's infrastructure inventory doc.
- This root is the *only* root that runs with the local backend. Every environment root from now
  on uses the S3 backend.

## Security notes

- State files can contain **secrets** (passwords, endpoint details). That is why the bucket is
  versioned, SSE-KMS encrypted, and has public access blocked. Do not weaken those settings.
- The DynamoDB lock table should be encrypted (AWS-managed key is the default) and never publicly
  accessible.
- Access to this bucket should be limited to the `platform-admins` group and the GitHub OIDC roles
  (added in step 08 / `terraform/account`).
