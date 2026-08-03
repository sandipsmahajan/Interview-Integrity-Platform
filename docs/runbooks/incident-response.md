# Runbook: Incident Response

**Symptom.** Anything that doesn't match an existing runbook, or a suspected platform-wide /
security incident.

**Severity.** Assigned during triage (below).

**Impact.** Depends on the incident; the goal is containment first.

## When to use this runbook

- An alert fired and no other runbook matches.
- More than one component is failing.
- A security event is suspected (unusual access, data exposure, unexpected infra changes).

## 1. Triage (first 5 minutes)

| Severity | Definition | Example | First action |
|---|---|---|---|
| **P1** | Platform down or data loss/leak | All services 5xx; RDS failed; credentials leaked | Page on-call + team; contain immediately |
| **P2** | Major feature degraded | Reports delayed; Kafka lag | Assign owner; contain within 30 min |
| **P3** | Minor | One pod crash-looping harmlessly | Ticket; fix in business hours |

Assign a single **incident commander** and a **scribe**. Announce in the incident channel:

```text
INCIDENT <ID> - P1 - <symptom> - owner: <name> - started: <UTC time>
```

## 2. Gather (next 10 minutes)

```bash
# What changed recently? (deploys, config, infra)
git log --oneline -20
helm history integrity --namespace integrity
kubectl -n integrity get events --sort-by=.lastTimestamp | tail -50

# Current state
kubectl -n integrity get pods,deploy,svc
kubectl get nodes

# Telemetry
# - Grafana: platform overview (error rate, latency)
# - CloudWatch: alarms, RDS/Kafka/Redis metrics
# - Logs: CloudWatch Logs Insights for the failing service (monitoring.md)
```

## 3. Contain (as soon as the symptom is understood)

**Stop the bleeding before the root cause.** Options in order of preference:

```bash
# 1. Roll back a bad release
helm rollback integrity <revision> --namespace integrity --reuse-values

# 2. Restart a stuck service
kubectl -n integrity rollout restart deployment/<service>

# 3. Scale out to absorb load
kubectl -n integrity scale deployment/<service> --replicas=5

# 4. Block bad traffic (prod WAF) / revoke a compromised credential
#    (security incident: see security.md + secret-rotation.md)
```

## 4. Diagnose (after containment)

Follow the relevant runbook now that the platform is stable:

| Symptom | Runbook |
|---|---|
| Pods crashing | `pod-crash.md` |
| Database | `database-failure.md` |
| Kafka | `kafka-failure.md` |
| Redis | `redis-failure.md` |
| Nodes | `node-failure.md` |
| Certificates | `certificate-renewal.md` |

If the diagnosis needs data:

```bash
# Trace a failing request (Jaeger/X-Ray): look for the slow/errored span
# Logs: pull the exact error
kubectl -n integrity logs deploy/<service> --tail=200 | grep -iE 'error|exception'
```

## 5. Resolve and verify

- Apply the fix from the runbook.
- Verify with the smoke test (`deployment/17-verify-platform.md` Level 4) **and** the original
  symptom.
- Announce resolution:

```text
INCIDENT <ID> - RESOLVED - <UTC time> - containment: <action> - root cause: <cause>
```

## 6. Post-incident (within 48 hours)

Write the postmortem (blameless):

```markdown
# Postmortem INCIDENT-<ID>
## Summary            # what happened, in 3 sentences
## Impact             # users, duration, RTO/RPO affected
## Timeline           # UTC times: detect -> triage -> contain -> diagnose -> fix
## Root cause         # 5-whys
## Contributing       # missing alarm, untested rollback, config drift
## Actions            # [ ] tickets with owners and dates
## Runbook delta      # [ ] what changed in docs/runbooks
```

Rules:

- No blame; the system failed, not the person.
- Every action is a tracked ticket.
- If a runbook was missing a step, add it the same shift.

## Escalation

| Timebox | Escalate to |
|---|---|
| P1: 15 min without containment | Platform/SRE lead |
| P1: 30 min | On-call + engineering manager |
| Security incident | CTO + security owner immediately |

## Prevention

- Monthly pager drill (fire a synthetic P1).
- Quarterly full DR drill using `disaster-recovery.md`.
- Postmortem actions become runbook/checklist items — track closure.
