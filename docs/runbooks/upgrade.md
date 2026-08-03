# Runbook: Upgrade

**Symptom.** Scheduled upgrade of images, the Helm chart, EKS, or Terraform providers.

**Severity.** P3 (planned). Goes P1 if a rollback is not rehearsed.

**Impact.** Per the component upgraded; rolling upgrades are zero-downtime.

## Prerequisites

- A maintenance window for cluster-level upgrades; approval for prod.
- Rollback rehearsed in qa first.

## 1. Image upgrade (routine release)

Happens automatically via `deploy.yml`. Manual path:

```bash
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml \
  --set <service>.image.tag=<new-sha>
kubectl -n integrity rollout status deployment/<service>
```

Roll forward one environment at a time: dev → qa → uat → prod, same SHA.

## 2. Helm chart upgrade

```bash
helm lint infra/helm/interview-integrity                     # syntax
helm template infra/helm/interview-integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml > /tmp/render.yaml
kubectl -n integrity diff -f /tmp/render.yaml                # review the diff

helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml
helm history integrity --namespace integrity                # record the revision
```

## 3. EKS cluster upgrade (one minor at a time)

```bash
# 1. Update eks_cluster_version in variables.tf/tfvars (e.g. 1.31 -> 1.32)
cd terraform/environments/<env>
terraform plan        # review node group + control plane changes
terraform apply

# 2. Upgrade tooling to a compatible minor
kubectl version --client

# 3. Verify
kubectl -n integrity get pods        # all Ready
kubectl -n integrity get nodes       # all Ready, versions match
```

Sequence: dev → qa → uat → prod, one minor per maintenance window, never skipping versions.

## 4. Terraform provider upgrade

```bash
cd terraform/environments/<env>
terraform init -upgrade
terraform plan        # review carefully; providers can reformat resources
terraform apply
```

Do `dev` first; a provider diff you did not expect means stop and investigate before touching
prod.

## 5. RDS / MSK / ElastiCache upgrades

```bash
# RDS minor/major version
aws rds describe-db-engine-versions --engine postgres --engine-version <target>
# modify (major upgrades require a maintenance window; check support for the target version)
aws rds modify-db-instance --db-instance-identifier integrity-<env> \
  --engine-version <target> --allow-major-version-upgrade --apply-immediately

# MSK / ElastiCache: use the console maintenance windows or the AWS CLI equivalents,
# in a maintenance window, one environment at a time.
```

Take a **manual RDS snapshot** before any major upgrade.

## Verification

```bash
# All services healthy after each stage
kubectl -n integrity get pods | grep -c '1/1'    # 19
# Smoke test (deployment/17-verify-platform.md Level 4)
# Dashboards: error rate and latency return to baseline within the window
```

## Rollback

| Upgrade | Rollback |
|---|---|
| Images | Deploy the previous SHA (`helm ... --set <service>.image.tag=<prev>`) |
| Helm chart | `helm rollback integrity <revision> --namespace integrity --reuse-values` |
| EKS version | Downgrade is not supported by EKS — restore from a **pre-upgrade snapshot** of nodes/backup, or rebuild the cluster at the old version and re-deploy |
| Terraform provider | Revert the version constraint in `versions.tf`, `terraform init -upgrade`, re-apply |

For EKS/DB major upgrades, always plan the "exit" before starting: a restore path and the
previous state backup.

## Prevention

- Read upgrade notes (EKS changelog, RDS major version docs) before starting.
- Rehearse rollback in qa in the same week as the upgrade.
- Keep a runbook-revision alongside every upgrade so the next person knows what changed.
