# Security Guide

**Purpose.** The operational security reference for Integrity Pro. It maps every security control
to who is responsible, how it is verified, and what to do when something is suspected of failing.
Architecture rationale: [`architecture/security.md`](architecture/security.md).

## 1. Control inventory and owners

| Control | Where it lives | Owner | Verified by |
|---|---|---|---|
| IAM (human) | AWS IAM (`platform-admins`, `integrity-deployer`) | Platform lead | Step 20 checklist §1 |
| OIDC CI roles | `terraform/iam` | Platform lead | `aws iam list-roles` |
| Secrets | AWS Secrets Manager + K8s Secret | Platform lead | Step 20 checklist §2 |
| TLS / certificates | ACM + ALB | Platform lead | `openssl s_client` |
| Network policies | `infra/k8s/network-policy.yaml` | SRE | `kubectl get netpol` |
| Security groups | `terraform/modules/networking` | SRE | `aws ec2 describe-security-groups` |
| RBAC | Kubernetes Role/RoleBinding | SRE | `kubectl auth can-i` |
| Encryption at rest | KMS keys via `terraform/modules/kms` | SRE | `aws kms describe-key` |

## 2. IAM

### Principles

1. **Least privilege**: every identity gets only what it needs.
2. **Groups, not users**: permissions attach to groups; people join groups.
3. **No static keys in CI**: GitHub Actions uses OIDC.
4. **MFA everywhere** for humans.

### The identity map

```text
humans        -> IAM user (MFA)                -> console/CLI
CI/CD         -> GitHub OIDC role per env       -> terraform + deploy
node groups   -> IAM role (node-level)          -> pull images, read EC2 metadata
pods (IRSA)   -> ServiceAccount role annotation -> narrow AWS API per workload
```

### Verification

```bash
# What can this role actually do? (find any overly-broad grants)
aws iam list-attached-role-policies --role-name integrity-prod-github-actions
aws iam get-role-policy --role-name <role> --policy-name <p>

# Who can touch the prod state bucket?
aws s3api get-bucket-policy --bucket integrity-terraform-state
```

## 3. Secrets

| Secret | Storage | Injected via | Rotation |
|---|---|---|---|
| JWT key | Secrets Manager | `integrity-secrets` → env | Lambda, 90 d (prod) |
| RDS master + per-service DB pw | Secrets Manager | `integrity-secrets` → env | Lambda (prod) |
| MSK SCRAM | Secrets Manager | `integrity-secrets` → env | manual/rotation (prod) |
| Redis token | Secrets Manager | `integrity-secrets` → env | manual |

Rules and verification:

- `git grep -riE '(AKIA|password.*=.*[A-Za-z0-9]{12})' --not` should find nothing (the CI
  workflow runs a secret scanner).
- Kubernetes Secrets are base64 only — encryption at rest comes from EKS KMS; RBAC restricts
  readers.
- If a secret is suspected exposed: rotate immediately
  (`runbooks/secret-rotation.md`), then audit access.

## 4. Certificates and TLS

- ACM issues and auto-renews; the ALB terminates TLS; HSTS on the ingress.
- Verify:
  ```bash
  # TLS 1.2+ enforced (tls1_1 handshake must FAIL)
  echo | openssl s_client -connect api.<env>...:443 -tls1_1 -servername api.<env>... 2>&1 | grep -c 'protocol' || echo "no tls1_1 = good"
  ```
- CAA record restricts issuers to `amazonaws.com`:
  ```bash
  dig +short <domain> CAA
  ```
- Certificates are in scope of the 14-day expiry alarm.

## 5. Network policies (cluster)

`infra/k8s/network-policy.yaml` is default-deny. Verify with:

```bash
kubectl -n integrity get networkpolicy
# integrity-default-deny-ingress / -egress + explicit allows
```

If a workload needs a new egress (e.g. a new external API), add a policy rule and get review —
opening egress is a security change, not a config tweak.

## 6. Security groups (AWS)

| Resource | Allowed ingress |
|---|---|
| ALB | `443` from internet |
| Nodes | `30080` from ALB SG only |
| RDS | `5432` from app SG |
| MSK | `9092/9094/9096` from app SG |
| ElastiCache | `6379` from app SG |

Verify no accidental exposure:

```bash
aws ec2 describe-security-groups --query \
  'SecurityGroups[?IpPermissions[?contains(IpRanges[].CidrIp, `0.0.0.0/0`)]].{GroupName:GroupName,Perms:IpPermissions}' \
  --output table
# Review each 0.0.0.0/0 row — only the ALB 443 (and public subnets) may have it
```

## 7. RBAC (Kubernetes)

| Subject | Permissions |
|---|---|
| Human operators | `view` + targeted `exec/logs` in `integrity` |
| Deploy pipeline | deploy-time rights (image, configmap/secret apply, rollout) via the chart's ServiceAccount |
| Services | nothing by default (IRSA for AWS APIs only) |

Check:

```bash
kubectl -n integrity get role,rolebinding
kubectl auth can-i --as=system:serviceaccount:integrity:<sa> get pods --namespace integrity
```

Never grant `cluster-admin` to a human or pipeline by default.

## 8. Least privilege in practice

| Ask | Minimum permission |
|---|---|
| Read logs | `logs/pods` + `cloudwatchlogs:GetLogEvents` |
| Deploy one service | `deployments/update` on that Deployment |
| Read a secret | `secretsmanager:GetSecretValue` on that secret only |
| Terraform an env | the `integrity-<env>-github-actions` OIDC role (not admin) |

When a request asks for more, push back with a resource-scoped policy instead.

## 9. Encryption

| At rest | In transit |
|---|---|
| S3 buckets (SSE-KMS), EBS (KMS), RDS/MSK/ElastiCache (KMS), Secrets Manager (KMS), K8s Secrets (KMS) | TLS 1.2+ everywhere; SASL/TLS on MSK; `ssl=true` on the RDS JDBC URL; HTTPS to S3/SES |

Verify one key is actually encrypting:

```bash
aws rds describe-db-instances --query 'DBInstances[].{DB:DBInstanceIdentifier,StorageEncrypted:StorageEncrypted}'
aws ec2 describe-volumes --query 'Volumes[].{Vol:VolumeId,Encrypted:Encrypted}'
```

## 10. Security incident response

1. Contain: revoke/rotate, isolate (network policy deny), pause deploys.
2. Use `runbooks/incident-response.md` (security variant).
3. Preserve evidence: snapshot the DB, preserve logs (CloudWatch), record `kubectl get events`.
4. Postmortem with the security owner; track actions to closure.

Suspected compromise quick-list:

| Indicator | Action |
|---|---|
| Unknown IAM role/user | `aws iam list-users`, review new in last 90 d |
| Odd CloudTrail API calls | `aws cloudtrail lookup-events` from unusual IPs |
| Pod making unexpected egress | Check netpol; inspect `kubectl logs` + metrics |
| Secret in Git | Rotate the secret + `git log -p` to understand exposure window |
| Unauthorized console login | Enable/check `aws iam get-credential-report` for recent logins |

## 11. Regular security hygiene

| Cadence | Activity |
|---|---|
| Weekly | `Step 20` checklist §1–§3 re-run |
| Monthly | Secret-scan CI output review; IAM audit; `kubectl auth can-i` spot checks |
| Quarterly | ECR/container vulnerability scan review; OIDC role policy review |
| Per release | `Step 20` §4 workloads; SBOM stored |
| Annually | Full threat-model refresh (`architecture/security.md`) |
