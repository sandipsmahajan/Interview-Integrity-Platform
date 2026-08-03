# Bootstrap

One-time bootstrap that creates the remote-state backend used by every
environment root.

## Resources created

| Resource | Purpose |
| --- | --- |
| `S3 bucket` `integrity-terraform-state-<account-id>` | Holds every environment's `terraform.tfstate` |
| `DynamoDB table` `integrity-terraform-locks` | State locking (prevents concurrent `apply`) |
| `KMS key` `alias/integrity/terraform-state` | Encrypts state at rest (SSE-KMS) |

## Usage

```bash
cd terraform/bootstrap

# Requires AWS credentials for the account (profile, env vars or OIDC role)
terraform init
terraform apply -var="state_bucket_name=<unique-bucket-name>"
```

The bucket name is only customised when the default `integrity-terraform-state-<account-id>`
collides with another account (bucket names are globally unique).

After the first successful apply, the other roots automatically use the
backend via their `backend.tf`. The KMS key ARN can be wired into the
`backend.tf` `kms_key_id` field for defence in depth (optional; SSE-S3 is
already enabled for the DynamoDB table and the bucket uses SSE-KMS).
