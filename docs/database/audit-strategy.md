# Audit Strategy

Two complementary mechanisms provide complete change tracking:

## 1. In-database history tables (per service)

High-value transactional entities keep an immutable `*_history` table written
by an AFTER trigger. History rows are never updated or deleted.

| Database | History table | Trigger source |
|----------|---------------|----------------|
| identity | `users_history` | `users` |
| organization | `organizations_history` | `organizations` |
| recruiter | `recruiters_history` | `recruiters` |
| candidate | `candidates_history` | `candidates` |
| interview | `interviews_history` | `interviews` |
| policy | `policies_history` | `policies` |
| report | `reports_history` | `reports` |
| storage | `storage_objects_history` | `storage_objects` |
| feature_flag | `feature_flags_history` | `feature_flags` |
| integration | `integrations_history` | `integrations` |
| configuration | `configuration_history` | `configurations` (INSERT/UPDATE) |
| scheduler | `job_executions` | append-only run log |
| telemetry | `telemetry_event_summaries` | immutable rollups |

Every history row records `history_action` (`INSERT`/`UPDATE`/`DELETE`),
`changed_by` (from the `app.user_id` connection setting or the row's
`updated_by`/`deleted_by`), `changed_at`, a full snapshot of the tracked
columns, and the row `version`. DELETE snapshots are written by the soft-delete
path (hard deletes are not used on these tables).

Actor capture example:

```sql
COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid)
```

## 2. Central audit trail (audit_db)

All services publish structured audit events to the `audit.*` Kafka topics.
The audit-service consumes them and persists `audit_events` plus field-level
`audit_event_changes`.

```sql
audit_events (
    id, organization_id, actor_id, actor_type,
    action, resource_type, resource_id,
    outcome,           -- SUCCESS | FAILURE | DENIED
    occurred_at, request_id, ip_address, user_agent, metadata
)
```

The schema is **append-only**; retention is enforced by partition lifecycle.
`audit_events` is partitioned by month so archival is a partition detach.

## 3. API access log

The API gateway writes one `api_audit_log` row per request (method, path,
status, duration, actor, request id, client IP). Partitioned by month with a
90-day retention; used for operational forensics and abuse detection.

## 4. Immutable key changes (violations / policy versions)

- `policy_versions` stores the immutable JSON definition of a policy at each
  version, so the exact rules that produced a violation are always
  reconstructible.
- `violation_reviews` and `violation_escalations` record every human decision
  with actor and timestamp.
- `object_versions` keeps every stored-object version when bucket versioning
  is enabled.

## Change-data lifecycle

| Table set | Retention | Mechanism |
|-----------|-----------|-----------|
| `*_history` | indefinite (tiered to archive) | trigger-written |
| `audit_events` | 7 years (compliance) | partition detach + archive |
| `api_audit_log` | 90 days | partition drop |
| `job_executions` | 12 months | age-based cleanup job |
| `configuration_history` | indefinite | trigger-written |

## Integrity properties

- **Write-once**: history/audit tables have no update path in application code.
- **Tamper evidence**: audit rows include `request_id` (correlation id) so a
  single request's effect can be replayed end-to-end from gateway to services.
- **Failure semantics**: audit publication is asynchronous (Kafka, at-least-once
  with idempotent consumers keyed on `event_id`), so an audit outage never
  blocks the business transaction.
