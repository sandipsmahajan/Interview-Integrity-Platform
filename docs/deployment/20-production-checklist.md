# 20 — Production Readiness Checklist

**Purpose.** The final sign-off before (or shortly after) going live: a comprehensive checklist
that verifies the platform is production-ready, secure, observable, and recoverable. Run it for
`uat` and `prod`.

## Prerequisites

- Steps 01–19 completed.
- At least one full `uat` deployment that passed step 17.

## Estimated Time

1–2 hours for a first pass; 30 minutes for repeat passes.

## How to use this checklist

Each item has a check, a command or evidence, and the "fail" action. **Every box must be ticked
before declaring the environment production-ready.** Record the checklist output in the release
documentation.

---

### 1. Identity and access

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 1.1 | Root account MFA enabled and root unused for daily work | IAM → Security credentials | Enable MFA; rotate root password |
| 1.2 | No static AWS keys in GitHub | `grep -r AKIA .github/` returns nothing | Rotate the key, move to OIDC |
| 1.3 | GitHub OIDC roles exist per env | `aws iam list-roles \| grep integrity-<env>-github-actions` | Apply `terraform/account` |
| 1.4 | Human IAM access uses groups + MFA | Console check | Fix membership |
| 1.5 | No `*` (all resources) policies on humans beyond bootstrap | `aws iam list-attached-user-policies` | Reduce to scoped policies |

### 2. Secrets

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 2.1 | No secrets in Git | `grep -riE '(password|secret|token)' --include='*.tfvars' --include='*.env' .` returns only `.example` files | Rotate anything found |
| 2.2 | Secrets Manager holds all prod secrets | `aws secretsmanager list-secrets` | Create/import missing |
| 2.3 | Secrets rotation enabled on prod (JWT, RDS, SCRAM) | Secret list shows `rotationEnabled: true` | Enable rotation |
| 2.4 | K8s Secrets encrypted at rest | EKS cluster `encryptionConfig` set | Re-apply `eks` module |

### 3. Network and TLS

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 3.1 | Data stores have no public SG ingress | `aws ec2 describe-security-groups` — RDS/MSK/Redis SGs only allow app SG | Fix SG rules |
| 3.2 | NodePort firewalled to ALB SG | SG for nodes on `30080` limited | Fix SG rules |
| 3.3 | HTTPS enforced (HTTP → 301) | `curl -sI http://<host>` returns redirect | Re-apply `alb` module |
| 3.4 | TLS 1.2+ only | `openssl s_client -tls1_1` fails | Set ALB policy |
| 3.5 | CAA record exists | `dig +short example.com CAA` → `amazonaws.com` | Add CAA |
| 3.6 | WAF enabled on prod ALB | `aws wafv2 list-web-acls` | Apply with `enable_waf=true` |
| 3.7 | NetworkPolicy default-deny active | `kubectl -n integrity get netpol` | Apply `infra/k8s/network-policy.yaml` |

### 4. Workloads

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 4.1 | All 19 deployments `1/1` Ready | `kubectl -n integrity get deploy` | See `runbooks/pod-crash.md` |
| 4.2 | Resource requests/limits set on every container | `kubectl -n integrity get deploy -o yaml \| grep -c resources` | Fix chart values |
| 4.3 | Readiness/liveness probes set | `kubectl get deploy -o jsonpath='{.items[*].spec.template.spec.containers[*].readinessProbe}'` non-empty | Fix chart |
| 4.4 | HPA active on scalable services | `kubectl -n integrity get hpa` | Fix chart |
| 4.5 | PodDisruptionBudgets set | `kubectl -n integrity get pdb` | Fix chart |
| 4.6 | Containers run non-root | `kubectl -n integrity get deploy -o yaml \| grep runAsNonRoot` | Fix image/chart |
| 4.7 | No `CrashLoopBackOff`/`ImagePullBackOff` in 7 days | `kubectl -n integrity get events --sort-by=.lastTimestamp` | Investigate |

### 5. Data durability

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 5.1 | RDS automated backups on, retention ≥ 7 days | `aws rds describe-db-instances --query 'DBInstances[].BackupRetentionPeriod'` | Enable |
| 5.2 | PITR enabled (backup window set) | Same command → `BackupRetentionPeriod > 0` | Enable |
| 5.3 | Multi-AZ on prod RDS | `DBInstances[].MultiAZ` = true | Promote to multi-AZ |
| 5.4 | Deletion protection on prod RDS | `DBInstances[].DeletionProtection` = true | Enable |
| 5.5 | S3 buckets versioned | `aws s3api get-bucket-versioning` per bucket | Enable |
| 5.6 | S3 `force_destroy=false` | `aws s3api get-bucket-policy` / tfvars | Guard against wipe |
| 5.7 | Restore drill passed this quarter | DR runbook output | Schedule one |
| 5.8 | RTO/RPO recorded | See `disaster-recovery.md` §RTO/RPO | Fill in numbers |

### 6. Observability

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 6.1 | Prometheus scrapes all services | Prometheus targets show 19 up | Fix scrape config/annotations |
| 6.2 | Grafana dashboards load | Dashboard URL renders | Provision dashboards |
| 6.3 | CloudWatch log groups exist per service | `aws logs describe-log-groups \| grep integrity` | Ship logs |
| 6.4 | Alerts fire and route (pager test) | Fire a test alarm | Wire Alertmanager/SNS |
| 6.5 | Cert expiry alarm exists | CloudWatch alarms list | Add alarm |
| 6.6 | Kafka lag alarm exists | Alarms list | Add alarm |

### 7. Operations

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 7.1 | On-call runbooks printed/accessible offline | `runbooks/` linked in the on-call doc | Add link |
| 7.2 | All 14 runbooks reviewed by a second engineer | Sign-off in PR | Review |
| 7.3 | Rollback tested in uat this quarter | `helm rollback` drill log | Schedule drill |
| 7.4 | Terraform state locked and accessible to the team | One engineer can `terraform plan` in uat | Fix IAM |
| 7.5 | Cost budget + alerts set | Budgets page | Create |

### 8. Compliance / misc

| # | Check | Evidence | Fail action |
|---|---|---|---|
| 8.1 | Data residency requirement met (region) | Region = intended | Document exception |
| 8.2 | Audit trail of platform actions enabled | `audit-service` DB growing; CloudTrail on | Enable CloudTrail |
| 8.3 | Software bill of materials (SBOM) per image | ECR/scan output stored | Generate SBOM |
| 8.4 | Documented incident response owner | On-call roster | Assign |

---

## Final sign-off

```text
Environment:  [prod]
Date:         [YYYY-MM-DD]
Reviewed by:  [name]
All boxes ticked:  [yes/no]
Exceptions (with owner + date):  [list]
```

## If something fails

Do **not** tick it. Fix it, re-run the affected section, and only then tick. For the common
failures, the right runbook is:

| Failed section | Runbook |
|---|---|
| 3.x network | `runbooks/ingress-issues.md`, `networking.md` |
| 4.x workloads | `runbooks/pod-crash.md`, `runbooks/service-rollback.md` |
| 5.x durability | `disaster-recovery.md` |
| 6.x observability | `monitoring.md` |
| 7.3 rollback | `runbooks/service-rollback.md` |
