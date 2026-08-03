# Troubleshooting: Redis Issues

**Symptom.** `NOAUTH`, `connection refused`, `READONLY`, OOM errors, heavy eviction, or sessions
being lost. See `runbooks/redis-failure.md` for the response version.

## 1. `NOAUTH Authentication required`

**Cause:** the client has no token, or the token differs from the server's.

**Diagnose:**

```bash
# what the services have
kubectl -n integrity get secret integrity-secrets -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d
# what the server expects (prod)
aws secretsmanager get-secret-value --secret-id integrity/<env>/redis-auth
```

**Fix:** align the secret, then `kubectl -n integrity rollout restart deployment --all`. If the
server-side token was rotated in ElastiCache, update Secrets Manager first (keep them in sync).

## 2. `connection refused` to `redis:6379` (dev)

**Diagnose:**

```bash
kubectl -n integrity get deploy redis
kubectl -n integrity rollout status deployment/redis
```

**Fix:** recreate: `kubectl -n integrity rollout restart deployment/redis`.

## 3. `READONLY You can't write against a read-only replica`

**Cause:** the config points at a read-replica endpoint instead of the primary.

**Fix:** use `terraform output redis_endpoint` (primary) and update `spring.data.redis.host`.

## 4. `OOM command not allowed when used memory > 'maxmemory'`

**Cause:** cache full and eviction policy is `noeviction` (or default).

**Diagnose:**

```bash
kubectl -n integrity exec -it deploy/redis -- redis-cli CONFIG GET maxmemory-policy
kubectl -n integrity exec -it deploy/redis -- redis-cli INFO memory | grep -E 'used_memory|maxmemory'
```

**Fix:**

```bash
# set a sane eviction policy (dev) - allkeys-lru evicts least-recently-used
kubectl -n integrity exec -it deploy/redis -- redis-cli CONFIG SET maxmemory-policy allkeys-lru
# prod: set it in the ElastiCache parameter group so it survives restarts
# also consider raising the node type (runbooks/scaling.md)
```

## 5. Session/refresh-token loss

**Symptom:** users logged out at odd times.

**Cause:**

- Pod restart with the Deployment (dev) — Redis is a Deployment with ephemeral storage.
- Cache flush / node replacement on ElastiCache.
- Token TTL shorter than intended.

**Fix:** if you need durability for refresh tokens, that's a design change (documented in
`architecture/sequence.md` §1) — the platform deliberately keeps them ephemeral and revocable.
For dev, accept loss on restart; for prod, confirm the cluster is multi-AZ and tokens' TTLs are
short enough that loss is benign.

## 6. High evictions / latency

**Diagnose:**

```bash
kubectl -n integrity exec -it deploy/redis -- redis-cli INFO stats | grep -i evicted
# prod: CloudWatch Evictions, CacheCPU, CurrConnections
```

**Fix:** evictions indicate pressure — raise the node type or tighten TTLs. Latency from
`latencystat` can point at a big-key scan (`--bigkeys`).

## Prevention

- Alarms on `Evictions > 0`, `FreeableMemory < 20%`.
- Never put long-lived data in Redis; it is a cache.
- Keep Redis auth in Secrets Manager as the single source.
