# Operations Manual

**Purpose.** The day-to-day operations reference for the Integrity Pro platform: normal operating
procedures, change windows, capacity, and how to use the runbook library. Pair it with the
[`runbooks/`](../runbooks/README.md) index for incident response.

## 1. Normal operating procedures (NOP)

### 1.1 Morning checks (SRE)

```bash
kubectl -n integrity get pods                       # all 1/1
kubectl -n integrity get hpa                        # CPU% sane, no FailedGetResourceMetric
kubectl -n integrity get pvc                        # no PVCs stuck Pending
kubectl get nodes                                   # all Ready, not over ~80% allocatable CPU
```

Then check dashboards: platform overview error rate, Kafka lag, RDS connections. Record anything
unusual in the ops log.

### 1.2 Release day

1. Verify `master` CI is green (`.github/workflows/ci.yml`).
2. Let `deploy.yml` deploy to `dev`; watch rollout + smoke test.
3. Promote the same image SHA to `qa`, then `uat` (approval), then `prod` (approval).
4. Run `deployment/17-verify-platform.md` Level 4 on `prod`.
5. Update the release notes: image SHAs, config changes, runbook diffs.

### 1.3 Change windows

- **No change window required** for rolling deploys (they are health-gated).
- **Planned maintenance window** for: cluster upgrades, RDS major upgrades, MSK broker changes,
  Terraform `destroy`. Schedule `dev` → `prod` across windows, never parallel in prod.

## 2. Capacity management

| Signal | Threshold | Action |
|---|---|---|
| Node allocatable CPU/mem > 80% sustained | 15 min | Raise `eks_node_max_size`, apply |
| HPA at max replicas sustained | 1 hour | Raise HPA max in values, or scale nodes |
| Kafka consumer lag rising but bounded | — | Monitor; add consumer replicas |
| Kafka lag unbounded | 10 min | See `runbooks/kafka-failure.md` |
| RDS `FreeStorageSpace < 20%` | alarm | Add storage (modify instance) or purge |
| ElastiCache `CacheCPU > 80%` | alarm | Larger node type or split caches |

Scaling procedures are in `runbooks/scaling.md`.

## 3. The runbook library

All incidents start here:

| Runbook | For |
|---|---|
| [`runbooks/service-restart.md`](../runbooks/service-restart.md) | Restart a healthy-but-stuck service |
| [`runbooks/pod-crash.md`](../runbooks/pod-crash.md) | CrashLoop / OOM / ImagePullBackOff |
| [`runbooks/database-failure.md`](../runbooks/database-failure.md) | RDS/Postgres down, slow, or corrupt |
| [`runbooks/kafka-failure.md`](../runbooks/kafka-failure.md) | Broker/consumer/topic problems |
| [`runbooks/redis-failure.md`](../runbooks/redis-failure.md) | Cache down / auth / OOM |
| [`runbooks/node-failure.md`](../runbooks/node-failure.md) | Node down, NotReady, autoscaler broken |
| [`runbooks/scaling.md`](../runbooks/scaling.md) | Scale services or cluster capacity |
| [`runbooks/certificate-renewal.md`](../runbooks/certificate-renewal.md) | ACM/cert-manager certificate renewal |
| [`runbooks/password-rotation.md`](../runbooks/password-rotation.md) | Rotate a database/user password |
| [`runbooks/secret-rotation.md`](../runbooks/secret-rotation.md) | Rotate JWT/SCRAM/Redis secrets |
| [`runbooks/backup-restore.md`](../runbooks/backup-restore.md) | Manual snapshot, PITR restore, S3 restore |
| [`runbooks/upgrade.md`](../runbooks/upgrade.md) | Helm chart, images, EKS, Terraform upgrades |
| [`runbooks/service-rollback.md`](../runbooks/service-rollback.md) | Roll back a bad release |
| [`runbooks/incident-response.md`](../runbooks/incident-response.md) | How to run an incident end-to-end |

## 4. Incident response protocol (summary)

1. **Identify** — what's the symptom? Who's affected? Check `runbooks/incident-response.md`.
2. **Triage** — severity: P1 (platform down / data loss), P2 (feature degraded), P3 (cosmetic).
3. **Mitigate before root-cause** — restart/rollback/scale to restore service *first*.
4. **Diagnose** — logs + metrics + traces (see `monitoring.md`).
5. **Fix** — apply the runbook; verify; monitor for recurrence.
6. **Learn** — write/update a runbook; open follow-up tickets; update the SLO review.

## 5. Operational checklist (per environment)

- [ ] 19 services Ready, probes passing
- [ ] Prometheus targets 19 up; Grafana dashboards load
- [ ] CloudWatch alarms armed (not muted)
- [ ] Backups running (RDS snapshot times + S3 versioning)
- [ ] Secrets rotation scheduled (prod) / last rotation verified
- [ ] Certificate expiries > 30 days
- [ ] Runbooks up to date (reviewed with the last incident)

## 6. Escalation matrix

| Level | When | Who |
|---|---|---|
| L1 | Alert fires, runbook resolves it | On-call engineer |
| L2 | Runbook doesn't resolve; cross-service issue | Platform/SRE lead |
| L3 | Data loss / security incident | On-call + CTO/security |

Escalate after **15 minutes** without mitigation for P1, 30 minutes for P2. Always announce
escalation in the incident channel.

## 7. Security notes

- All operational commands use least-privilege RBAC roles (see `security.md`).
- Anything destructive (`destroy`, secret rotation, SQL writes) requires a second person and a
  change record.
- Never perform prod changes from a shared shell; use the pipeline or a named human with MFA.
