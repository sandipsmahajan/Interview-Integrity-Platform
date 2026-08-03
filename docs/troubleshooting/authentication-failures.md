# Troubleshooting: Authentication Failures

**Symptom.** Login fails (`401/403`), tokens rejected, refresh fails, RBAC denials, or email
verification tokens don't work.

## 1. Login returns `401 Unauthorized`

**Diagnose:**

```bash
# Service-side error
kubectl -n integrity logs deploy/identity-service --tail=50 | grep -iE 'auth|token|password'

# Is identity-service itself healthy?
kubectl -n integrity get pods | grep identity-service
kubectl -n integrity logs deploy/identity-service --tail=30 | grep -iE 'redis|datasource'
```

| Cause | Fix |
|---|---|
| Wrong password (user's) | Nothing to fix; log the attempt for audit |
| Seed user missing | Re-seed (`identity-service` seed data; verify in the DB) |
| Redis down (refresh-token store) | `runbooks/redis-failure.md` |
| DB down (user lookup) | `runbooks/database-failure.md` |
| `JWT_SECRET` differs across pods | Ensure all services read the **same** `JWT_SECRET` from `integrity-secrets`; restart all |

## 2. Login succeeds but subsequent calls are `401/403`

**Cause:** token validation fails — signing key mismatch, expiry, or clock skew.

**Diagnose:**

```bash
# Decode the token locally (no secret needed for the header/payload)
TOKEN=<token>
echo $TOKEN | cut -d. -f2 | base64 -d | jq .   # check exp, iss, aud
date -u                                          # compare with exp
```

**Fix:**

- **exp < now**: token expired — refresh flow should handle this; if refresh fails see §3.
- **Secret mismatch**: `JWT_SECRET` rotated only on some pods (rotation without full rollout).
  Restart everything and confirm one value: `kubectl -n integrity get secret integrity-secrets -o
  jsonpath='{.data.JWT_SECRET}'`.
- **Clock skew**: `date` differs by minutes → fix NTP on nodes.

## 3. Refresh token fails

**Cause:** refresh token missing/expired in Redis, or Redis flushed.

**Diagnose:**

```bash
kubectl -n integrity exec -it deploy/redis -- redis-cli SCAN 0 MATCH '*refresh*' | head
```

**Fix:** users must re-login (acceptable, documented behavior). If refresh fails for *everyone*,
check `REDIS_PASSWORD`/endpoint (§1 map).

## 4. `403` for a specific role/endpoint

**Cause:** authorization (not authentication) — the user's role lacks the permission, or RBAC
(RBAC in the service, not K8s) rejects.

**Diagnose:**

```bash
# who is the user, what roles do they hold?
kubectl -n integrity logs deploy/api-gateway --tail=50 | grep -iE '403|authoriz'
```

**Fix:** correct the role assignment in `identity-service`/`organization-service`; verify the
gateway route requires the expected scope (see `api.md`).

## 5. OIDC / IAM-based auth failures (infra side)

**Symptom:** CI or IRSA can't assume a role (`AccessDenied` when calling AWS APIs).

**Diagnose:**

```bash
# IRSA: is the pod's token mapped to a role?
kubectl -n integrity get sa <service> -o yaml | grep annotations
# CI: check the workflow's OIDC step error; verify the role trust policy
aws iam get-role --role-name integrity-<env>-github-actions
```

**Fix:**

- IRSA: ensure the ServiceAccount has `eks.amazonaws.com/role-arn` and the role trust policy
  allows the OIDC provider's `sub` for that SA.
- CI: `terraform/account` OIDC provider must exist; repository/org must match the trust policy.

## 6. SES / email verification token fails

**Symptom:** verification emails never arrive, or links 404.

**Diagnose:**

```bash
# Mailpit (dev): open http://localhost:8025 and read the raw link
# Prod: check notification-service logs + SES delivery
kubectl -n integrity logs deploy/notification-service --tail=30 | grep -iE 'ses|mail'
```

**Fix:** verify SES is out of sandbox (`aws ses get-account-sending-enabled`); check the
notification service's `spring.mail.*` / SES region config; validate the token URL host matches
`portal.<env>...`.

## Prevention

- One `JWT_SECRET` source (Secrets Manager) + full rollout on rotation (`secret-rotation.md`).
- Short access-token TTL + revocable refresh tokens (already the design).
- Alarm on auth-failure rate spikes (brute-force watch) and on `401` ratio.
- Log auth decisions (success/failure, actor, reason) via `audit-service`.
