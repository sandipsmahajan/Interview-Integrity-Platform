# Troubleshooting: Helm Failures

**Symptom.** `helm upgrade/install` fails, renders wrong resources, or a `helm rollback` is
needed.

## 1. `Error: unable to build kubernetes objects from release manifest`

**Cause:** template syntax error or invalid YAML in the chart (often a values-type mismatch).

**Fix:**

```bash
helm lint infra/helm/interview-integrity                      # syntax + schema check
helm template integrity infra/helm/interview-integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml > /tmp/render.yaml
# inspect /tmp/render.yaml around the error; fix the template/values, then re-run helm
```

## 2. `Error: release integrity failed, and has been uninstalled` / `has failed`

**Cause:** an apply-time failure (invalid resource, forbidden object, wrong field).

**Diagnose:**

```bash
helm history integrity --namespace integrity
kubectl -n integrity get events --sort-by=.lastTimestamp | tail -30
```

**Fix:**

```bash
# inspect what would change before re-applying
helm template integrity infra/helm/interview-integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml | kubectl -n integrity diff -f -

# re-run with --atomic so Helm auto-rolls back on failure:
helm upgrade integrity infra/helm/interview-integrity --namespace integrity \
  -f infra/helm/interview-integrity/values-<env>.yaml --atomic
```

## 3. `Error: values don't meet the specifications of the schema`

**Cause:** a `values-<env>.yaml` value violates `values.schema.json` (if present) or a template
expects a certain type.

**Fix:** read the schema/values, correct the value type (string vs number vs list), and re-lint.

## 4. `Error: unknown flag: --reuse-values`

**Cause:** Helm 2 (old), which this repo does not support.

**Fix:** install Helm 3 (`helm version`), then re-run.

## 5. Rollback doesn't restore previous behavior

**Symptom:** after `helm rollback`, the app still looks wrong.

**Cause:** `--reuse-values` was omitted, so Helm re-used the *chart* defaults instead of the
applied values; or the configmap/secret changed independently.

**Fix:**

```bash
helm rollback integrity <revision> --namespace integrity --reuse-values
# confirm the ConfigMap/Secret match the revision:
helm get values integrity --namespace integrity
kubectl -n integrity get configmap integrity-config -o yaml | grep -c '<key>'
```

## 6. `configmap "integrity-config" not found` / `secret "integrity-secrets" not found`

**Cause:** the chart references objects the pipeline normally creates, but they were deleted or
never created on a manual deploy.

**Fix:** recreate them (`deployment/15-deploy-services.md` Step 1), then `helm upgrade`.

## 7. HPA reports `FailedGetResourceMetric`

**Cause:** metrics-server not installed (Helm isn't at fault; Kubernetes is).

**Fix:** install metrics-server (`kubernetes.md` §9), then check `kubectl -n integrity get hpa`.

## Prevention

- `helm lint` + `helm template` in CI (already in `ci.yml`).
- Use `--atomic` for pipelines so failures roll back automatically.
- Never hand-edit a deployed release's values; change the values file and re-upgrade.
