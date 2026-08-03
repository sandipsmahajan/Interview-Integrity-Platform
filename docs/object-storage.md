# Object Storage Strategy

The storage-service manages object metadata, versions and signed URLs. It
talks to an S3-compatible store through the `platform.storage.endpoint`
property — so the same code runs against MinIO in development and Amazon S3 in
qa/uat/prod.

## The Abstraction

`libs/config/.../PropertyKeys.java` defines the endpoint key:

```java
public static final String STORAGE_ENDPOINT = PLATFORM_PREFIX + ".storage.endpoint"; // platform.storage.endpoint
```

Each Spring profile sets it:

| Profile | `PLATFORM_STORAGE_ENDPOINT` |
| --- | --- |
| `local` | `http://localhost:9000` (MinIO) |
| `docker` | `http://minio:9000` |
| `kubernetes` / `dev` | `http://minio.integrity.svc.cluster.local:9000` (MinIO) |
| `qa` / `uat` / `prod` | `https://s3.<region>.amazonaws.com` |

## Buckets

The platform uses three data buckets (plus two operational buckets):

| Bucket | Purpose | Versioning | Lifecycle |
| --- | --- | --- | --- |
| `integrity-<env>-documents` | signed documents, consent records | on | IA 90d → GLACIER 180d |
| `integrity-<env>-reports` | generated reports | on | IA 90d → GLACIER 180d |
| `integrity-<env>-uploads` | user uploads | on | IA 90d → GLACIER 180d |
| `integrity-<env>-flowlogs` | VPC flow logs | on | expire 90d |
| `integrity-<env>-alb-logs` | ALB access logs | on | expire 30d |

## Provisioning

### Kubernetes dev (`infra/k8s/minio.yaml`)
MinIO Deployment + headless Service, PVC on `gp3-encrypted`, and a one-shot
`minio-seed-buckets` Job that creates `documents`, `reports`, `uploads`
(idempotent with `mc mb --ignore-existing`).

### EKS qa/uat/prod (Terraform `terraform/modules/s3`)
- Buckets named `<name_prefix>-<bucket>` (e.g. `integrity-prod-documents`).
- Versioning, lifecycle transitions, KMS encryption (S3-managed key alias
  `integrity-<env>/s3`).
- Public access blocked; policy allows only the platform role / ALB log
  delivery principal.

## Access Control

- **MinIO (dev)**: credentials in the `minio-credentials` Secret
  (`integrity` / `integrity-secret`), mounted by the storage-service.
- **S3 (prod)**: the storage-service uses IRSA (IAM Roles for Service
  Accounts) — the pod's ServiceAccount (`storage`) is annotated with
  `eks.amazonaws.com/role-arn` and the role policy grants
  `s3:GetObject` / `s3:PutObject` / `s3:DeleteObject` on the three data
  buckets only.

## Signed URLs

The storage-service generates presigned URLs so clients can upload/download
objects directly without streaming through the API. Presigned URLs work
identically for MinIO and S3 (both implement the S3 presigned-URL protocol);
only the endpoint and credentials differ.

## Migration Notes

- **Object keys** should be namespace-independent (use content hashes / UUIDs)
  so a MinIO → S3 migration requires only a bucket copy, not key rewriting.
- `aws s3 sync s3://integrity-dev-documents s3://integrity-prod-documents`
  (or `mc mirror`) migrates data; version history is preserved.
- After migration, rotate `PLATFORM_STORAGE_ENDPOINT` to S3 and the IRSA role —
  no code change.
