# Troubleshooting: Terraform Failures

**Symptom.** `terraform init/plan/apply` errors: lock issues, backend problems, provider
failures, or unexpected diffs.

## 1. `Error acquiring the state lock`

```text
Error: Error acquiring the state lock
```

**Cause:** another `terraform apply` holds the DynamoDB lock (someone else is applying, a CI job
is running, or a crashed process left a stale lock).

**Diagnose:**

```bash
# Who/what holds it? The error prints the lock ID and holder info.
# Check the lock table for the item:
aws dynamodb get-item --table-name integrity-terraform-locks \
  --key '{"LockID":{"S":"integrity/<env>/terraform.tfstate-md5"}}'
```

**Fix:**

- If a real apply is running: wait for it.
- If stale (no apply running): `terraform force-unlock <lock-id>` in the env root.
- Only force-unlock when you are certain no apply is active — it can corrupt state otherwise.

## 2. `Backend initialization required`

```text
Error: Backend initialization required: please run "terraform init"
```

**Cause:** `.terraform/` was cleaned or `backend.tf` changed.

**Fix:**

```bash
cd terraform/environments/<env>
terraform init
```

## 3. `Initializing provider plugins` failure

```text
Error: Failed to query available provider packages
```

**Cause:** network/proxy to `registry.terraform.io`, or a missing `versions.tf` constraint.

**Fix:**

```bash
# check connectivity
curl -sI https://registry.terraform.io | head -1
# set proxy if needed
export HTTPS_PROXY=http://<proxy>:<port>
terraform init -upgrade
```

## 4. Provider version conflicts

```text
Error: Invalid provider version
```

**Cause:** Terraform/state expects a provider range the lockfile doesn't satisfy.

**Fix:**

```bash
terraform init -upgrade   # refresh the lockfile
terraform plan            # review the diff
```

## 5. `AuthorizationError` / `AccessDenied` during apply

**Cause:** IAM propagation (role created moments ago) or the CLI identity lacks a permission.

**Diagnose:**

```bash
aws sts get-caller-identity   # confirm identity
# look for the specific action in the error, e.g. ec2:CreateVpc
```

**Fix:** wait 30–60 s and retry (EKS/RDS IAM is eventual). If persistent, add the missing
action to the executing role and re-apply.

## 6. KMS key errors (EKS/RDS/ECR)

```text
Error: KMS.DisabledException  /  "not authorized to use kms key"
```

**Cause:** key policy missing the resource, or the key was disabled.

**Fix:**

```bash
aws kms describe-key --key-id alias/integrity/terraform-state   # check KeyState
aws kms get-key-policy --key-id <key> --policy-name default      # check Principal grants
# re-apply; KMS changes propagate in ~60 s
```

## 7. Unexpected plan deletions

**Cause:** a variable changed (e.g. a CIDR or `kafka_enabled`), or a module refactor left orphaned
resources.

**Diagnose:**

```bash
terraform state list | grep -i <suspected-resource>
```

**Fix:**

- If it's a refactor: `terraform state mv <old> <new>` (see `terraform.md` §12).
- If it's an accidental var change: revert the variable and re-plan.
- Never accept a delete-laden plan you don't understand.

## 8. State corrupted / backend drift

**Symptom:** `terraform state` shows resources that don't exist, or apply wants to recreate
everything.

**Diagnose:**

```bash
terraform state list > /tmp/before.txt
terraform plan          # compare resource count with reality
```

**Fix:** restore the previous state version from the S3 bucket (versioning is on):

```bash
aws s3api list-object-versions --bucket integrity-terraform-state \
  --prefix integrity/<env>/terraform.tfstate
# download the last-known-good versionId
aws s3api get-object --bucket integrity-terraform-state \
  --key integrity/<env>/terraform.tfstate --version-id <versionId> \
  /tmp/terraform.tfstate
# then restore via backend state (init -backend-config + state push) or re-apply from plan
```

## Prevention

- Always plan with `-out=` and review it before apply.
- Never delete the state bucket or lock table.
- Keep the CI pipeline as the normal apply path; manual applies are reviewed exceptions.
