# Index Strategy

Every table's indexes are created in its `V1__init_schema.sql`. The strategy
covers the five workload classes the platform must serve: search, filtering,
sorting, reporting/analytics, and high-volume time-series inserts.

## Principles

1. **Tenant-first composites** - `organization_id` is the leading column of
   every composite index on tenant-scoped tables, because RLS filters and
   application queries always narrow by tenant first.
2. **Partial indexes for soft-delete** - unique business keys index only live
   rows (`WHERE deleted_at IS NULL`), which also shrinks the index.
3. **Partial indexes for hot states** - the most common filtering predicate is
   compiled into the index (`WHERE status = 'PENDING'`,
   `WHERE status IN ('ACTIVE','PAUSED')`).
4. **Expression indexes** - `lower(email)` / `lower(domain)` make uniqueness
   and lookup case-insensitive.
5. **Covering indexes** - frequently projected columns are included with
   `INCLUDE` so index-only scans answer hot queries.
6. **GIN for JSONB and arrays** - containment lookups on `payload`,
   `condition`, `metadata`, `skills`, `scopes`.
7. **BRIN for time series** - `telemetry_events` uses a BRIN index on
   `(occurred_at, seq)`, which is orders of magnitude smaller than B-tree and
   perfect for correlated insert order.

## Search

| Table | Index | Purpose |
|-------|-------|---------|
| `users` | `(organization_id, lower(email))` partial unique | email lookup and uniqueness |
| `candidates` | `(organization_id, full_name)`, `(organization_id, lower(email))` | name/email search |
| `storage_objects` | GIN `(metadata jsonb_path_ops)` | object metadata search |
| `policy_rules` | GIN `(condition jsonb_path_ops)` | rule predicate search |
| `violations` | GIN `(evidence jsonb_path_ops)` | evidence lookup |
| `interviews` | GIN `(metadata jsonb_path_ops)` | custom interview fields |

## Filtering / hot states

| Table | Index | Predicate compiled in |
|-------|-------|----------------------|
| `notifications` | `(organization_id, status, scheduled_at)` | `status = 'PENDING'` |
| `violations` | `(organization_id, status, occurred_at DESC)` | `status != 'DISMISSED'` |
| `interview_sessions` | `(status)` | `status IN ('ACTIVE','PAUSED')` |
| `telemetry_sessions` | `(status)` | `status IN ('STARTED','ACTIVE')` |
| `reports` | `(organization_id, status, requested_at DESC)` | full filter |
| `signed_urls` | `(expires_at)` | `revoked_at IS NULL` |
| `scheduled_jobs` | `(next_run_at)`, `(handler)` | `status = 'ENABLED'` |
| `feature_flags` | `(organization_id, environment, enabled)` | full filter |

## Sorting / time-window queries

- `(organization_id, created_at DESC)` patterns on `notifications`,
  `candidate_documents`, `recruiter_notes`, `job_executions`.
- `(organization_id, started_at DESC)` on `telemetry_sessions`,
  `integration_sync_logs`, `job_executions`.
- `(candidate_id, created_at DESC)` on `recruiter_notes` supports per-candidate
  activity feeds.

## Reporting / analytics

- Daily summary tables key on `(summary_date, <dimension>)` and additionally
  index `<dimension>_id` for entity drill-downs.
- Weekly views and monthly materialized views aggregate the daily tables so
  dashboard latency is independent of raw event volume.
- `mv_monthly_*` are indexed on `(organization_id, summary_month DESC)`.

## High-volume inserts (telemetry)

Per monthly partition:

- `BRIN (occurred_at, seq) WITH (pages_per_range = 128)` - compact range index
  with near-zero insert overhead.
- `GIN (payload jsonb_path_ops)` - payload containment.
- `(session_id, event_type, seq) INCLUDE (payload)` - covering index for the
  session replay path.
- `(organization_id, occurred_at)` - tenant + time filtering.

The BRIN index must be created **per partition** (it is, by
`telemetry_create_partition()`), keeping index maintenance off the insert
critical path.

## Index maintenance

- All indexes are created in migrations with `CREATE INDEX` (not
  `CREATE INDEX CONCURRENTLY`) for simplicity in the reference deployment.
  Large-table index additions in production should use
  `CREATE INDEX CONCURRENTLY` in dedicated migrations.
- `ANALYZE` runs on each new telemetry partition at creation; the rest of the
  database relies on autovacuum defaults tuned per service.
- Regularly review `pg_stat_user_indexes` for unused indexes
  (`idx_scan = 0` across a retention window) and drop them in a versioned
  migration.

## Cost examples

- Unique user lookup by email: index-only on the partial expression index.
- Open-violation triage queue: partial index returns the queue without
  touching the table.
- Session replay (thousands of events): covering index on
  `(session_id, event_type, seq)`.
- Monthly dashboard: materialized view scan, constant time regardless of raw
  event count.
