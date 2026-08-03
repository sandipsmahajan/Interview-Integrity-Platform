# Runbooks

**Purpose.** The incident-response library for Integrity Pro. Each runbook is a self-contained,
step-by-step procedure. **Always mitigate before root-cause**, and update the runbook after every
incident.

## Severity definitions

| Severity | Definition | Target response |
|---|---|---|
| **P1** | Platform down or data loss | Mitigate in < 15 min |
| **P2** | Feature degraded (e.g. reports delayed) | Mitigate in < 30 min |
| **P3** | Cosmetic / no user impact | Next working day |

## Incident flow

1. Pick the runbook matching the symptom below.
2. If none matches, use [`incident-response.md`](incident-response.md).
3. Mitigate (restart/rollback/scale) → diagnose → fix → verify → learn.

## The 14 runbooks

| # | Runbook | Symptom | Severity typical |
|---|---|---|---|
| 1 | [`service-restart.md`](service-restart.md) | A service is healthy but misbehaving; needs a clean restart | P2 |
| 2 | [`pod-crash.md`](pod-crash.md) | CrashLoopBackOff, OOMKilled, ImagePullBackOff | P1/P2 |
| 3 | [`database-failure.md`](database-failure.md) | RDS down, slow queries, connection exhaustion, PITR | P1 |
| 4 | [`kafka-failure.md`](kafka-failure.md) | Broker down, consumer lag, no events flowing | P1/P2 |
| 5 | [`redis-failure.md`](redis-failure.md) | Cache down, NOAUTH, OOM, evictions | P2 |
| 6 | [`node-failure.md`](node-failure.md) | Node NotReady, autoscaler not scaling | P2 |
| 7 | [`scaling.md`](scaling.md) | Need more capacity (pods or nodes) | P2 |
| 8 | [`certificate-renewal.md`](certificate-renewal.md) | Cert expiry alarm, browser warnings | P2 |
| 9 | [`password-rotation.md`](password-rotation.md) | Rotate a database/user password | P2 |
| 10 | [`secret-rotation.md`](secret-rotation.md) | Rotate JWT / SCRAM / Redis secrets | P2 |
| 11 | [`backup-restore.md`](backup-restore.md) | Manual snapshot, restore from backup/PITR | P1 |
| 12 | [`upgrade.md`](upgrade.md) | Upgrade images, chart, EKS, Terraform | P3 (planned) |
| 13 | [`service-rollback.md`](service-rollback.md) | A release is bad — revert it | P1 |
| 14 | [`incident-response.md`](incident-response.md) | Anything not covered above | any |

## Every runbook follows the same structure

- **Symptom / Severity / Impact**
- **Prerequisites** (tools, permissions, time)
- **Diagnosis** (commands to confirm the cause)
- **Resolution** (ordered steps; each command explained)
- **Verification** (how you know it's fixed)
- **Rollback** (how to undo the resolution)
- **Prevention** (how to avoid recurrence)

## Common tools (defined once)

| Tool | Where it's documented |
|---|---|
| `kubectl -n integrity` | `kubernetes.md` |
| `helm` | `deployment/06-*`, `kubernetes.md` §8 |
| `terraform` | `terraform.md` |
| `aws` CLI | `deployment/03-*` |
| Logs / metrics / traces | `monitoring.md` |

## Rule for writing a new runbook

If you handled an incident and the runbook library was missing a step, **write the step into the
runbook in the same shift** — future on-call will thank you. Runbooks are code: reviewed in PRs,
versioned in Git.
