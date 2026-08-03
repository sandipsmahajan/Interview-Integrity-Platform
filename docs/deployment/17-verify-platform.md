# 17 — Verify the Platform End to End

**Purpose.** To prove, systematically, that the deployed platform is genuinely working — not just
"pods are running". This is the gate before you connect a real domain (step 18).

## Prerequisites

- Steps 08–16 completed.
- `kubectl` configured; `helm` installed.
- Your AWS CLI authenticated.

## Estimated Time

20 minutes.

## Required AWS permissions

Read access to the cluster and AWS services (already granted).

## Level 1 — Cluster and workloads

```bash
# Nodes are Ready
kubectl get nodes
# 3/3 Ready (or your env's node count)

# All 19 deployments are healthy
kubectl -n integrity get deploy
kubectl -n integrity get pods
# READY 1/1 on every row; no CrashLoopBackOff / ImagePullBackOff
```

If anything is not `1/1`, read its logs before continuing:

```bash
kubectl -n integrity logs deploy/<broken-service> --tail=50
```

## Level 2 — Data plane connectivity

### PostgreSQL

```bash
kubectl -n integrity exec -it postgres-0 -- psql -U integrity -d postgres -c \
  "SELECT count(*) FROM pg_database WHERE datname LIKE '%_db';"
# 16
```

### Redis

```bash
kubectl -n integrity exec -it deploy/redis -- redis-cli ping
# PONG
```

### Kafka (dev/local — Strimzi)

```bash
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | wc -l
# 13
```

### Object storage

```bash
# dev/local MinIO buckets were seeded by the minio-seed-buckets Job
kubectl -n integrity get jobs
# minio-seed-buckets   1/1   Complete
```

## Level 3 — Service registration and routing

```bash
# Registry: every service registered
kubectl -n integrity port-forward svc/discovery-service 8761:8761 &
sleep 2
curl -s http://localhost:8761/eureka/apps | grep -o '<app>.*</app>' | sed 's/<[^>]*>//g' | sort
# 19 application names
kill %1
```

Then prove routing through the gateway:

```bash
kubectl -n integrity port-forward svc/api-gateway 8080:8080 &
sleep 2
curl -s http://localhost:8080/actuator/health | jq .status
# "UP"
kill %1
```

## Level 4 — End-to-end business flow

Do this through the ingress/ALB path if it's up, otherwise via port-forward. The minimal
functional proof:

```bash
# 1. Health
curl -s https://api.<env>.integritypro.example.com/actuator/health   # or the ALB DNS + Host header

# 2. Login (proves gateway -> identity -> DB -> Redis)
TOKEN=$(curl -s -X POST https://api.<env>.../api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<seeded-admin>","password":"<seeded-password>"}' | jq -r .accessToken)
echo "login ok: ${#TOKEN} chars"

# 3. Authenticated call (proves JWT validation + routing)
curl -s https://api.<env>.../api/v1/candidates \
  -H "Authorization: Bearer $TOKEN" | jq '. | type'
```

Each step exercises a different layer (see `architecture/sequence.md`):

| Check | Proves |
|---|---|
| Login | Gateway + identity + PostgreSQL + Redis |
| `GET /api/v1/candidates` | JWT validation in `libs/security` + routing + candidate DB |
| Email delivery | notification-service + SES/Mailpit (see logs) |
| Kafka event on interview | telemetry → policy path (see logs + topic offsets) |

## Expected output

A clean run of Levels 1–4 with no errors, the login returns a non-empty token, and the
authenticated call returns JSON.

## Verification checklist (copy into your ticket/PR)

```text
[ ] kubectl get nodes -> all Ready
[ ] kubectl -n integrity get pods -> all 1/1
[ ] 16 databases present
[ ] Redis PONG
[ ] 13 Kafka topics
[ ] 19 apps registered in discovery
[ ] gateway health "UP"
[ ] login returns token
[ ] authenticated GET returns JSON
```

## Common errors and troubleshooting

| Check | Failure | First action |
|---|---|---|
| Pods not Ready | See `runbooks/pod-crash.md` | `kubectl describe pod`, `kubectl logs` |
| DB count < 16 | Databases not created | Step 13 |
| Registry count < 19 | Service failed to register | Its config may point at the wrong discovery URL |
| Login 401 | Seed user missing / wrong secret | Check identity seed + `JWT_SECRET` consistency |
| Login 500 | DB/Redis unreachable from pod | Check SG/network policy for 5432/6379 |
| Portal blank | Portal build/route issue | `kubectl -n integrity logs deploy/recruiter-portal` (if present) or check Vite proxy config |

## Rollback procedure

This is a **verification** step — no rollback needed. If verification fails, roll back the
relevant release:

```bash
helm history integrity --namespace integrity
helm rollback integrity <revision> --namespace integrity --reuse-values
kubectl -n integrity rollout status deployment --all
```

## Best practices

- Run this checklist **before** and **after** every release (the `deploy.yml` smoke test
  automates a subset; the manual checklist covers the rest).
- Save the output of the checklist into the release notes for auditability.
- Repeat Level 4 after any data-plane change (rotation, restore, failover).

## Security notes

- Use the **seeded** low-privilege test user for smoke checks, not a production admin.
- Never paste real tokens/passwords into logs or tickets; redact them.
- All of the above commands that read secrets (RDS passwords etc.) should be avoided on shared
  shells; use the cluster-internal paths documented in steps 13–14.
