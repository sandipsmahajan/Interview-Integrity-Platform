# Runbook: Secret Rotation

**Symptom.** Rotation window due, a secret suspected compromised, or `integrity-secrets` needs
refreshing (JWT, SCRAM, Redis, MinIO keys).

**Severity.** P2 planned; P1 on confirmed leak.

**Impact.** Transient session/connection churn; JWT rotation logs users out (by design).

## Prerequisites

- Secrets Manager + deploy pipeline access; 30 minutes.

## Secrets covered here

| Secret | Effect of rotation | Restart scope |
|---|---|---|
| JWT signing key | All access tokens invalidated; refresh tokens revocable in Redis | all services |
| MSK SCRAM | Kafka clients re-authenticate | Kafka consumers/producers |
| Redis AUTH token | Cache clients re-authenticate | services using Redis |
| MinIO access/secret keys | Object storage clients re-authenticate | storage-service + seed jobs |

## Resolution

### Step 1 — Update the source of truth in Secrets Manager

```bash
# JWT
aws secretsmanager put-secret-value \
  --secret-id integrity/<env>/jwt \
  --secret-string "$(openssl rand -base64 48)"

# SCRAM (must match the broker-side credential; update MSK first if needed)
aws secretsmanager put-secret-value \
  --secret-id integrity/<env>/kafka-scram \
  --secret-string '{"username":"integrity","password":"<NEW>"}'

# Redis token
aws secretsmanager put-secret-value \
  --secret-id integrity/<env>/redis-auth \
  --secret-string "<NEW-TOKEN>"
```

### Step 2 — Update the broker/side that must match (SCRAM only)

For MSK SCRAM the secret must also exist in the MSK secret list (Terraform wires it):

```bash
cd terraform/environments/<env>
terraform apply -target=module.kafka -auto-approve   # re-asserts the SCRAM association
```

### Step 3 — Re-materialize the Kubernetes Secret and restart

The deploy pipeline reads Secrets Manager and writes `integrity-secrets`. Trigger it (re-run
`deploy.yml`), or do it manually:

```bash
kubectl -n integrity delete secret integrity-secrets
kubectl -n integrity create secret generic integrity-secrets \
  --from-literal=JWT_SECRET="$(aws secretsmanager get-secret-value --secret-id integrity/<env>/jwt --query SecretString --output text)" \
  # ... plus all other literals (see deployment/15-deploy-services.md Step 1)
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

> Because the deployment template carries a checksum of the Secret, simply updating the Secret and
> running `helm upgrade` also triggers the rollout automatically.

## Verification

```bash
# Services are healthy with the new secret
kubectl -n integrity get pods                     # all 1/1
kubectl -n integrity logs deploy/identity-service --tail=30 | grep -iE 'error|token'

# JWT: old tokens rejected, new login works
OLD_TOKEN=<stored-old-token>
curl -s https://api.<env>.../api/v1/... -H "Authorization: Bearer $OLD_TOKEN"
# expect 401
# new login -> new token -> 200 (deployment/17-verify-platform.md Level 4)

# Kafka SCRAM: consumers reconnected, no SASL errors
kubectl -n integrity logs deploy/policy-engine-service --tail=30 | grep -iE 'sasl|error'
```

## Rollback

- Restore the previous secret version in Secrets Manager:
  ```bash
  aws secretsmanager list-secret-versions --secret-id <secret-id>
  # note the VersionId of the previous version
  aws secretsmanager update-secret --secret-id <secret-id> \
    --secret-string "<previous-value>"
  ```
- Re-materialize the cluster Secret and restart all services (Step 3).

## Prevention

- Rotate on a calendar (prod: 90 days) and after any suspected exposure.
- Prefer the automated rotation Lambda (prod) — manual rotation is for qa/uat and emergencies.
- Keep a "last rotated" annotation on the secret and the ops checklist item in sync.
