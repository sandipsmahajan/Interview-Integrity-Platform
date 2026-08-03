# Runbook: Password Rotation

**Symptom.** Scheduled rotation is due, a credential is suspected of leaking, or a team member
with knowledge of a password left.

**Severity.** P2 (planned or precautionary); P1 if a leak is confirmed.

**Impact.** Brief connection churn for consumers while the new password propagates.

## Prerequisites

- Access to Secrets Manager, RDS admin, and the deploy pipeline.
- 30 minutes; schedule in a window.

## What "password" means here

| Credential | Stored where | Consumers |
|---|---|---|
| RDS master password | Secrets Manager `integrity/<env>/rds-master` | admin ops |
| Per-service DB passwords | Secrets Manager `integrity/<env>/db/<service>` | the service's pods |
| MSK SCRAM password | Secrets Manager `integrity/<env>/kafka-scram` | Kafka consumers |
| Redis AUTH token | Secrets Manager `integrity/<env>/redis-auth` | cache clients |

## Resolution

### A — Automated (prod): trigger the rotation Lambda

Prod secrets have an attached rotation Lambda. Trigger it manually:

```bash
aws secretsmanager rotate-secret --secret-id integrity/<env>/rds-master
```

Wait for completion and confirm a new version:

```bash
aws secretsmanager describe-secret --secret-id integrity/<env>/rds-master \
  --query 'RotationEnabled,LastRotatedDate'
```

### B — Manual rotation (DB password), step by step

1. **Generate a new strong password** (do not reuse):
   ```bash
   NEW_PW=$(openssl rand -base64 24)
   ```
2. **Update the database** (from the admin pod, as the master user):
   ```sql
   ALTER USER identity_service PASSWORD '<NEW_PW>';
   ```
3. **Store the new value in Secrets Manager** (creates a new version):
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id integrity/<env>/db/identity-service \
     --secret-string '{"username":"identity_service","password":"<NEW_PW>"}'
   ```
4. **Propagate to the cluster** (re-materialize `integrity-secrets` and restart):
   ```bash
   # via the deploy pipeline (re-run deploy.yml with the new secret), or manually:
   kubectl -n integrity rollout restart deployment/identity-service
   kubectl -n integrity rollout status deployment/identity-service
   ```
5. **Verify the old password no longer works** (optional hardening):
   ```bash
   PGPASSWORD=<OLD_PW> psql -h <rds> -U identity_service -d identity_db -c "SELECT 1"
   # should fail with password authentication failed
   ```

### C — Rotation of the JWT signing secret (see also secret-rotation.md)

Rotate the JWT key in Secrets Manager, re-materialize the Secret, and restart **all** services:

```bash
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

> Note: rotating the JWT key invalidates existing access tokens. Plan for a session reset
> (acceptable; refresh tokens are revocable in Redis).

## Verification

```bash
# The service connects with the new password
kubectl -n integrity logs deploy/<service> --tail=30 | grep -iE 'connect|error'
# no "password authentication failed"

# Business smoke test passes (login + authed call — deployment/17-verify-platform.md)
```

## Rollback

- If the new password breaks a consumer, restore the previous version in Secrets Manager
  (version rollback) and restart:
  ```bash
  aws secretsmanager list-secret-versions --secret-id <secret-id>
  aws secretsmanager update-secret --secret-id <secret-id> \
    --secret-string '{"username":"...","password":"<old>"}'
  kubectl -n integrity rollout restart deployment --all
  ```
- In RDS, `ALTER USER ... PASSWORD '<old>'` restores the database side.

## Prevention

- Automate rotation with the Lambda (prod) and verify it in qa first.
- Never share DB passwords; one user per service (already the design).
- Store rotation history in Secrets Manager versioning; never in Git.
