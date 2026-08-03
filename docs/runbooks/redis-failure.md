# Runbook: Redis Failure

**Symptom.** `NOAUTH`, `connection refused`, `READONLY`, `OOM command not allowed`, high
evictions, or slow cache reads. Login/sessions and hot-read caching degrade.

**Severity.** P2 (cache loss degrades but does not lose durable data; sessions may reset).

**Impact.** Refresh-token validation fails → users are logged out; hot-read paths hit the
database instead (higher latency, more DB load).

## Prerequisites

- `kubectl`, `aws` CLI, Terraform outputs.
- 15 minutes.

## Diagnosis

```bash
# dev: is the pod healthy?
kubectl -n integrity get deploy redis
kubectl -n integrity logs deploy/redis --tail=30

# prod: cluster state + metrics
aws elasticache describe-cache-clusters \
  --query 'CacheClusters[].{Id:CacheClusterId,Status:CacheClusterStatus,EngineVersion:EngineVersion}'
# CloudWatch: CacheCPU, Evictions, CurrConnections, FreeableMemory

# Service side
kubectl -n integrity logs deploy/identity-service --tail=50 | grep -iE 'redis|noauth'
```

## Resolution by cause

### A — `NOAUTH Authentication required`

The pod has no (or wrong) password.

```bash
# Confirm the secret value and restart consumers so they pick it up
aws secretsmanager get-secret-value --secret-id integrity/<env>/redis-auth
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

### B — `connection refused`

```bash
# dev: recreate the pod
kubectl -n integrity rollout restart deployment/redis

# prod: security group misconfig — confirm SG allows app SG on 6379
aws ec2 describe-security-groups --group-ids <elasticache-sg-id> \
  --query 'SecurityGroups[].IpPermissions[].{From:FromPort,To:ToPort,Groups:UserIdGroupPairs}'
```

### C — `READONLY ... replica`

The config points at a replica endpoint. Use the **primary** endpoint:

```bash
cd terraform/environments/<env>
terraform output redis_endpoint
# update the profile config's spring.data.redis.host accordingly and rollout
```

### D — OOM / heavy eviction

```bash
# dev: check policy and memory
kubectl -n integrity exec -it deploy/redis -- redis-cli CONFIG GET maxmemory
kubectl -n integrity exec -it deploy/redis -- redis-cli INFO stats | grep -i evicted_keys

# prod: resize the cache node
aws elasticache modify-cache-cluster \
  --cache-cluster-id <cluster> \
  --cache-node-type cache.m5.large --apply-immediately
```

Also review key TTLs: refresh tokens and session data must have short TTLs (the platform sets
them); long-lived blobs should move to object storage, not Redis.

## Verification

```bash
# dev
kubectl -n integrity exec -it deploy/redis -- redis-cli ping
# PONG

# prod (from an in-cluster pod)
kubectl -n integrity run redis-check --rm -it --image=redis:7 -- \
  redis-cli -h <primary-endpoint> -p 6379 -a "$REDIS_PASSWORD" ping
# PONG

# Users can log in again (session flow) — run the login smoke test
```

## Rollback

- Config change broke things: revert the Redis host/password in the profile config and rollout.
- A resize is not reversible instantly — scale back down only after confirming the previous size
  was sufficient.

## Prevention

- Alarm on `Evictions > 0` and `FreeableMemory < 20%` (prod).
- Keep refresh-token TTLs short so Redis never becomes the source of truth for long-lived data.
- Rotate the Redis auth token in qa before prod (`secret-rotation.md`).
