# Troubleshooting Guide

**Purpose.** The A-to-Z symptom → cause → fix reference for Integrity Pro. If you can't resolve
the issue with the relevant runbook, this is the second stop.

## How to use this guide

1. Find your symptom category below and open the guide.
2. Follow the diagnosis commands in order.
3. Apply the fix, then verify with the checklist in that guide.
4. If the runbook library lacked a step, add it (see `runbooks/README.md`).

## Index

| # | Guide | When to use |
|---|---|---|
| 1 | [`terraform-failures.md`](terraform-failures.md) | `terraform plan/apply/init` errors, state locks, provider issues |
| 2 | [`helm-failures.md`](helm-failures.md) | `helm upgrade` failures, chart render errors, rollback issues |
| 3 | [`kubernetes-failures.md`](kubernetes-failures.md) | Pods not ready, events, RBAC, HPA, PVC problems |
| 4 | [`flyway-failures.md`](flyway-failures.md) | Migration validation errors, checksum mismatch, stuck migrations |
| 5 | [`database-connection-issues.md`](database-connection-issues.md) | "Connection refused", auth failures, timeouts, pool exhaustion |
| 6 | [`kafka-issues.md`](kafka-issues.md) | No events, lag, LEADER_NOT_AVAILABLE, SASL errors, DLQ |
| 7 | [`redis-issues.md`](redis-issues.md) | NOAUTH, connection, eviction, session loss |
| 8 | [`ingress-issues.md`](ingress-issues.md) | 404/502/503, Host-header problems, ALB target health |
| 9 | [`dns-issues.md`](dns-issues.md) | NXDOMAIN, records not propagating, CNAME/ALIAS problems |
| 10 | [`ssl-certificate-issues.md`](ssl-certificate-issues.md) | Browser warnings, chain errors, renewal failures |
| 11 | [`authentication-failures.md`](authentication-failures.md) | Login 401/403, JWT expiry, role/RBAC failures, SES access |
| 12 | [`pipeline-failures.md`](pipeline-failures.md) | GitHub Actions CI/CD failures (build, terraform, deploy) |

## Quick reference matrix

| Symptom | Most likely cause | Guide → Runbook |
|---|---|---|
| `Error acquiring the state lock` | concurrent apply / stale lock | [`terraform-failures.md`](terraform-failures.md) |
| `release integrity failed: ...` | bad chart/values | [`helm-failures.md`](helm-failures.md) → `service-rollback.md` |
| Pod `CrashLoopBackOff` | config / dependency | [`kubernetes-failures.md`](kubernetes-failures.md) → `pod-crash.md` |
| `FlywayException: Validate failed` | edited an applied migration | [`flyway-failures.md`](flyway-failures.md) |
| `connection refused :5432` | DB down/SG/network policy | [`database-connection-issues.md`](database-connection-issues.md) → `database-failure.md` |
| `LEADER_NOT_AVAILABLE` | broker booting / topic missing | [`kafka-issues.md`](kafka-issues.md) → `kafka-failure.md` |
| `NOAUTH Authentication required` | wrong Redis token | [`redis-issues.md`](redis-issues.md) → `redis-failure.md` |
| Ingress `404` | wrong Host / missing rule | [`ingress-issues.md`](ingress-issues.md) |
| `NXDOMAIN` | DNS not published/propagated | [`dns-issues.md`](dns-issues.md) |
| `ERR_CERT_*` | expired/mismatched cert | [`ssl-certificate-issues.md`](ssl-certificate-issues.md) → `certificate-renewal.md` |
| Login `401` | JWT/seed-user/secret mismatch | [`authentication-failures.md`](authentication-failures.md) |
| Red workflow check | build/test/terraform/helm step failed | [`pipeline-failures.md`](pipeline-failures.md) |

## Debugging fundamentals (apply to every category)

1. **Change first**: what changed last (deploy, config, secret, infra)? `git log --oneline -10`,
   `helm history`.
2. **Read the logs**: `kubectl -n integrity logs <pod> --previous --tail=50`.
3. **Check the events**: `kubectl -n integrity get events --sort-by=.lastTimestamp | tail -30`.
4. **Verify the dependency**: is the thing it connects to actually reachable (DB/Kafka/Redis)?
5. **Restart vs root-cause**: restart is mitigation; find the cause before concluding.

## Common output-formatting note

Where commands pipe into `jq`, install it (`brew install jq` / `apt-get install -y jq`) — it is
used for readable JSON.
