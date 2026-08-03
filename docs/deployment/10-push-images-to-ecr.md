# 10 — Push the Images to Amazon ECR

**Purpose.** To upload the locally built images to Amazon ECR, where the EKS cluster pulls them
from during deployment.

## Prerequisites

- Steps 01–09 completed.
- Images tagged locally (step 09).
- AWS CLI authenticated (step 03).

## Estimated Time

10–30 minutes depending on image sizes and connection speed.

## Required AWS permissions

Your `platform-admins` user (or the GitHub OIDC role in CI) needs at least:

- `ecr:GetAuthorizationToken`
- `ecr:CreateRepository` (first push)
- `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`
- `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`

The repositories already exist from step 08 (`integrity-<env>/<service>`), so pushes only need the
upload permissions.

## What ECR is

Amazon Elastic Container Registry is AWS's Docker registry. Repositories are scoped per
environment (`integrity-dev/identity-service`), have **immutable tags** (a tag maps to exactly
one image — you cannot silently overwrite), **scan on push**, and are KMS-encrypted.

## Steps

### 1. Set the environment facts

```bash
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export REGION=us-east-1
export ENV=dev
export REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
echo "Registry: ${REGISTRY}"
```

### 2. Authenticate Docker to ECR

```bash
aws ecr get-login-password --region ${REGION} | \
  docker login --username AWS --password-stdin "${REGISTRY}"
```

**What this does:** exchanges your CLI credentials for a short-lived ECR password and logs Docker
in. Expected output: `Login Succeeded`.

> The password is valid for **12 hours** and is never stored on disk by Docker permanently — you
> must re-run this after the token expires (and CI does it on every run).

### 3. Retag the images with the full ECR URI

```bash
SHA=9cbbf32   # or your commit SHA
for svc in api-gateway discovery-service identity-service organization-service \
           recruiter-service candidate-service interview-service desktop-client-service \
           telemetry-service policy-engine-service report-service notification-service \
           analytics-service audit-service storage-service feature-flag-service \
           scheduler-service integration-service configuration-service; do
  docker tag "integrity-${ENV}/${svc}:dev" \
            "${REGISTRY}/integrity-${ENV}/${svc}:${SHA}"
done
```

**What this does:** points the same local image at the ECR URI with an immutable SHA tag. No data
is copied yet — tags are just names.

### 4. Push all images

```bash
for svc in api-gateway discovery-service identity-service organization-service \
           recruiter-service candidate-service interview-service desktop-client-service \
           telemetry-service policy-engine-service report-service notification-service \
           analytics-service audit-service storage-service feature-flag-service \
           scheduler-service integration-service configuration-service; do
  docker push "${REGISTRY}/integrity-${ENV}/${svc}:${SHA}"
done
```

Each service prints upload progress ending with `latest: digest: sha256:... size: ...`.

## Expected output

```text
integrity-<sha>: digest: sha256:1f2... size: 2345
...
```

The final verification:

```bash
aws ecr describe-images \
  --repository-name integrity-${ENV}/identity-service \
  --query 'imageDetails[].{Tag:imageTags,Digest:imageDigest}' --output table
```

You should see your `SHA` tag with a `sha256:` digest. Because tags are immutable, a second push
with the same tag **fails** — that is a feature, not a bug.

## Verification steps

1. The `describe-images` command above returns the tag.
2. ECR scanning reports no critical vulnerabilities:
   ```bash
   aws ecr describe-image-scan-findings \
     --repository-name integrity-${ENV}/identity-service \
     --image-id imageTag=${SHA} --query 'imageScanFindings.findingSeverityCounts' --output json
   ```
   Treat critical findings seriously before promoting to prod.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `denied: Your authorization token has expired` | 12-hour token expired | Re-run step 2 |
| `denied: Access Denied` | Missing ECR permissions or wrong registry | Verify `ACCOUNT_ID`/`REGION`; check IAM policy |
| `Repository ... does not exist` | Step 08 did not create that repo | `terraform apply` in the env root, or `aws ecr create-repository` |
| `Image with reference ... already exists` | Immutable tag collision | That's correct behavior — use a new SHA |
| Push is very slow | Large images / bandwidth | Push once per SHA; images are pulled by digest in prod |

## Rollback procedure

- To remove a bad push (rarely needed — ECR retains history):
  ```bash
  aws ecr batch-delete-image \
    --repository-name integrity-${ENV}/identity-service \
    --image-ids imageTag=${SHA}
  ```
- To roll a deployment back, you do **not** delete anything from ECR — you point the release at an
  older, already-pushed SHA (see `runbooks/service-rollback.md`).

## Best practices

- Push **one image set per commit SHA** and keep a mapping of `SHA → environment releases`
  (the deploy workflow records this in the release notes).
- Rely on the `deploy.yml` pipeline for pushes in the normal flow; manual pushes are only for
  bootstrapping or emergency fixes.
- ECR lifecycle policies keep the last 20 tags per repo (see `terraform/modules/ecr/main.tf`), so
  old SHAs expire automatically — don't fight that with manual deletions.

## Security notes

- ECR images are scanned on push (`scan_on_push` in the ECR module). Fix or quarantine critical
  vulnerabilities before they reach prod.
- Tags are immutable and repositories are KMS-encrypted — both are configured in Terraform and
  should never be relaxed.
- The EKS node roles pull images via IRSA-scoped permissions; never grant the node role more than
  `ecr:BatchGetImage` + `ecr:GetDownloadUrlForLayer` on the environment's repositories.
