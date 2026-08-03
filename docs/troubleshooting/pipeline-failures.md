# Troubleshooting: CI/CD Pipeline Failures

**Symptom.** A GitHub Actions workflow fails: CI build, Terraform plan/apply, or the deploy
pipeline.

## 1. CI workflow fails

```bash
# Re-run and open the failing step: .github/workflows/ci.yml
```

| Failing step | Typical cause | Fix |
|---|---|---|
| `gradle build` | Compile/test failure in a service or `libs` | Fix the code; run locally `./gradlew check` |
| `client` checks | Rust lint/build issue in `client/` | `cargo check` / `cargo clippy` in `client/` |
| `portal build` | Node/TS error in `portals/` | `npm run build` locally; check Node version (`>=24 <25`) |
| `terraform fmt/validate` | Unformatted or invalid `.tf` | `terraform fmt -recursive`, `terraform validate` in the root |
| `helm lint/template` | Chart/values error | `helm lint infra/helm/interview-integrity` |
| YAML parse of k8s/compose | A manifest is invalid YAML | `docker compose config` / `kubectl apply --dry-run=client` |

## 2. `terraform.yml` plan fails

| Symptom | Cause | Fix |
|---|---|---|
| `Error: Backend initialization required` | Cache cleaned between plan runs | `terraform init` in the workflow (already there) |
| `AuthorizationError` | OIDC role not created yet | Apply `terraform/account` once (`deployment/08-*` §A) |
| `Error acquiring state lock` | A manual apply or previous job holds it | Wait; force-unlock if stale (`terraform-failures.md` §1) |
| `Invalid provider version` | lockfile drift | `terraform init -upgrade` |
| PR comment "plan" missing | Permissions on the `GITHUB_TOKEN` | Add `pull-requests: write` to the workflow permissions |

## 3. `terraform.yml` apply fails

- **Approval gate**: the GitHub environment (`qa`/`uat`/`prod`) must be approved; a missing
  reviewer is not a failure.
- **Apply error mid-run**: apply is idempotent — fix the underlying issue (usually IAM
  propagation or a resource limit) and re-run.
- **EKS/RDS creation timeout**: AWS-side, wait and re-run.

## 4. `deploy.yml` fails

| Failing step | Cause | Fix |
|---|---|---|
| `docker build/push` | Docker build context / ECR auth | Verify `ACCOUNT_ID`/`REGION`; the workflow logs in to ECR via OIDC |
| `ecr:GetAuthorizationToken` denied | OIDC role policy missing ECR perms | Re-apply the `iam` module |
| ConfigMap/Secret render | A GitHub secret missing (`RDS_PASSWORD`, `JWT_SECRET`, …) | Add the secret to the repo/environment secrets |
| `helm upgrade` | Chart/values issue | `helm-failures.md` |
| `kubectl rollout status` timeout | Pods didn't become ready | `kubernetes-failures.md` §1; the pipeline auto-rolls back |
| Gateway smoke test | New release broke the API | Check gateway logs; the pipeline already rolled back — investigate before re-deploying |

## 5. Smoke test passes locally but fails in CI

Common causes:

- CI uses a different profile/env (e.g. smoke against the deployed cluster vs local).
- Secrets set locally but missing in the environment's secret store.
- The smoke host expects HTTPS and the test hit HTTP (or the `Host` header mismatch).

Fix: mirror CI's environment exactly (`deployment/17-verify-platform.md`), and keep the smoke
test host/header in sync with `values-<env>.yaml`.

## 6. Pipeline "skipped" unexpectedly

- Workflow triggered only on `master` and specific paths — check the `on:` triggers.
- Environment protection rules required a reviewer.

## Prevention

- Make CI the gate: nothing merges with a red CI (branch protection).
- Keep the smoke test strict but stable (no flaky timeouts).
- Log the image SHAs + Helm revision in every release note so a failing deploy maps to a rollback
  target instantly.
