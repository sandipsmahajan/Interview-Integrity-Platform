# 14 — Deploy Redis (cache and sessions)

**Purpose.** To provision the Redis tier that the platform uses for refresh-token storage,
session state, and hot-read caching.

## Prerequisites

- **dev/local**: EKS cluster ready (step 08).
- **qa/uat/prod**: ElastiCache provisioned in step 08 (Terraform output `redis_endpoint` +
  `redis_auth_secret_arn`).
- `kubectl` configured.

## Estimated Time

15 minutes.

## Required AWS permissions

- dev/local: cluster write.
- prod: none new — ElastiCache already exists; services just need the endpoint + auth secret.

## Which path applies to you

| Environment | Redis | Auth |
|---|---|---|
| `local` | Docker container | none (dev) |
| `dev` | In-cluster Redis Deployment (`infra/k8s/redis.yaml`) | none inside cluster |
| `qa`/`uat`/`prod` | Amazon ElastiCache for Redis | Redis AUTH token (from Secrets Manager) |

## Step A — dev/local: apply the in-cluster Redis

```bash
kubectl apply -f infra/k8s/redis.yaml
```

**What this does:** creates a small Redis Deployment + Service named `redis`, so services connect
to `redis:6379`. In `dev` the data is a Deployment (ephemeral Pod storage); for durability in
local use the Docker Compose container with its volume.

Wait and verify:

```bash
kubectl -n integrity rollout status deployment/redis
kubectl -n integrity exec -it deploy/redis -- redis-cli ping
# PONG
```

## Step B — qa/uat/prod: wire ElastiCache

Terraform created the cluster in step 08. You only need to make the endpoint and token available
to the services:

### 1. Confirm the endpoint and secret

```bash
cd terraform/environments/<env>
terraform output redis_endpoint          # e.g. integrity-<env>.xxxx.cache.amazonaws.com
terraform output redis_auth_secret_arn   # e.g. arn:aws:secretsmanager:...:secret:integrity/<env>/redis-auth-...
```

### 2. Ensure the Secret exists for the cluster

The deploy pipeline materializes `integrity-secrets` from GitHub secrets; the Redis token should
also be present in Secrets Manager (`integrity/<env>/redis-auth`) so rotation is a single action.
Confirm:

```bash
aws secretsmanager get-secret-value --secret-id integrity/<env>/redis-auth --query SecretString
```

### 3. Confirm services use the right config

Services read `spring.data.redis.*` from the profile config (see `configuration-reference.md`).
For `prod` this points at the ElastiCache endpoint with `password: ${REDIS_PASSWORD}`. After any
change:

```bash
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

## Expected output

- dev: `PONG` from `redis-cli ping`.
- prod: `redis_endpoint` output; no `NOAUTH`/`READONLY` errors in service logs.

## Verification steps

```bash
# dev
kubectl -n integrity exec -it deploy/redis -- redis-cli INFO keyspace

# prod — from a pod inside the cluster, using the token
kubectl -n integrity run redis-check --rm -it --image=redis:7 -- \
  redis-cli -h integrity-<env>.xxxx.cache.amazonaws.com -p 6379 -a "$REDIS_PASSWORD" ping
# PONG
```

Then confirm a service actually used Redis (e.g. login sets a refresh token):

```bash
kubectl -n integrity logs deploy/identity-service --tail=50 | grep -i redis
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `NOAUTH Authentication required` | Token missing/wrong in config | Set `REDIS_PASSWORD` from the secret; restart |
| `READONLY You can't write against a read-only replica` | Wrote to a replica endpoint | Use the primary endpoint, not a read replica |
| `connection refused: redis:6379` | Redis pod down in dev | `kubectl -n integrity rollout status deployment/redis` |
| `timed out connecting` in prod | Security group blocks 6379 | Allow app SG → ElastiCache SG on 6379 |
| `OOM command not allowed when used memory > 'maxmemory'` | Cache eviction pressure | Raise node type or set an eviction policy (see `runbooks/redis-failure.md`) |

## Rollback procedure

- **dev/local**: `kubectl delete -f infra/k8s/redis.yaml`.
- **prod**: point services back at the previous endpoint/config and restart. Cache loss is not
  data loss — the cache is re-populated from the database (that is its purpose).

## Best practices

- Treat Redis as a **cache**, not durable storage — session tokens are the exception, and their
  TTLs are short (see `sequence.md` §1).
- Use the ElastiCache primary endpoint in config and only add read replicas with app awareness
  (the platform is not replica-aware today — keep one cluster).
- Configure `maxmemory-policy` (e.g. `allkeys-lru`) deliberately, never the default no-eviction
  under memory pressure.

## Security notes

- Redis auth (prod) means an attacker who reaches port 6379 still cannot read data without the
  token; the token lives in Secrets Manager.
- Keep Redis in private subnets with a SG limited to the app security group — Redis has no access
  control by default beyond AUTH, so network isolation is your primary defense.
- Never expose the dev Redis via NodePort or LoadBalancer; it is cluster-internal only.
