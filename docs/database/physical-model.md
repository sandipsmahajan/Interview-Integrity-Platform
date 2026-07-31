# Physical Data Model

Physical storage decisions behind the logical model.

## Data types

| Domain | Type | Rationale |
|--------|------|-----------|
| Identifiers | `UUID` (`gen_random_uuid()`) | globally unique, no sequence contention, safe to merge across databases |
| Money | `BIGINT` in cents | integer arithmetic, no floating point drift |
| Scores / ratios | `NUMERIC(5,2)` | exact decimal, check-constrained to 0-100 |
| Timestamps | `TIMESTAMPTZ` | UTC-normalized; clients convert to local |
| Duration | `INT` seconds (check-constrained) | compact and index-friendly |
| IP addresses | `INET` | native compare/contains operators |
| Dynamic payload | `JSONB` | validated shapes with GIN indexing |
| Ordered sets | `TEXT[]` | skills, scopes, events; GIN indexed |
| Country code | `CHAR(2)` | fixed width |
| Long text | `TEXT` | no length cap, TOAST-compressed automatically |
| Row history ids | `BIGSERIAL` | monotonic ordering for history tables |

## ENUM types

Implemented as PostgreSQL native `ENUM` (not lookup tables) because the value
sets are small, stable, and enforcement happens at the column. ENUM storage is
a 4-byte OID reference, so columns stay compact. Extending an enum is a single
`ALTER TYPE ... ADD VALUE` migration.

## JSONB policy

JSONB is used where the shape is tenant-configurable or event-defined:

- `payload` on `telemetry_events` - free-form event data
- `condition` on `policy_rules` - serialized predicate
- `metadata` / `settings` / `config` / `variants` / `rules` / `parameters`
- `filters` on reports, `evidence` on violations

Each JSONB column that is filtered on has a GIN index
(`USING GIN (col jsonb_path_ops)`) for containment queries. JSONB is validated
at the service boundary and normalized before insert; the database enforces
`NOT NULL DEFAULT '{}'`.

## Sequences

`gen_random_uuid()` (built into PostgreSQL 13+) replaces the `uuid-ossp`
extension and sequence-based IDs everywhere except:

- `history_id BIGSERIAL` on in-DB history tables
- `id BIGSERIAL` on high-volume append logs (`api_audit_log`,
  `audit_event_changes`, `integration_sync_logs`, `notification_deliveries`)
- `id BIGSERIAL` on `analytics_job_runs`

## Partitioning (physical)

| Table | Partition key | Grain | Indexes per partition |
|-------|---------------|-------|-----------------------|
| `telemetry_events` | RANGE `(occurred_at)` | monthly | BRIN `(occurred_at, seq)`, GIN `(payload)`, covering `(session_id, event_type, seq) INCLUDE (payload)`, `(organization_id, occurred_at)` |
| `telemetry_event_summaries` | RANGE `(bucket_start)` | monthly | none extra |
| `audit_events` | RANGE `(occurred_at)` | monthly | `(organization_id, occurred_at)`, `(actor_id, occurred_at)`, `(resource_type, resource_id, occurred_at)` |
| `audit_event_changes` | RANGE `(occurred_at)` | monthly | none extra |
| `api_audit_log` | RANGE `(occurred_at)` | monthly | `(organization_id, occurred_at)`, `(method, path, occurred_at)`, `(request_id)` |

Partitioned tables include the partition key in the primary key because
PostgreSQL requires every unique index on a partitioned table to contain the
partitioning column. Partitions are created ahead of time by
`telemetry_ensure_partitions()` / `audit_create_*_partitions()` and dropped or
detached by retention policy.

## TOAST and row size

Large TEXT/JSONB values are TOAST-compressed automatically. To keep heap tuples
small for the highest-volume tables:

- `telemetry_events` stores compact event payloads (< 1 KB typical); large
  screenshots are stored in the object store (storage_db) and referenced from
  the event `payload` via a `storage_object_id`.
- BRIN indexes on the time columns keep partition-level metadata compact.

## Generated columns

No generated columns are currently required. If a derived value (for example
`bucket_start` rounded from `occurred_at`) becomes a hot query key, it should be
added as a `GENERATED ALWAYS AS ... STORED` column in a versioned migration and
indexed.

## Views

- `v_telemetry_session_counts` - per-session event counts (telemetry_db)
- `v_open_violations` - triage queue (policy_db)
- `weekly_organization_summaries`, `weekly_recruiter_summaries`,
  `weekly_integrity_summaries` - weekly rollups over daily tables (analytics_db)
- `mv_telemetry_event_type_stats` (telemetry_db) and
  `mv_monthly_organization_summaries`, `mv_monthly_integrity_summaries`
  (analytics_db) are `MATERIALIZED VIEW ... WITH NO DATA`, refreshed by
  scheduler-driven jobs.

## Tablespaces

Default tablespace is used in the reference implementation. For production
scale, plan dedicated tablespaces:

- `ts_fast` (NVMe) for `telemetry_events` current partitions and hot OLTP tables
- `ts_analytics` for `analytics_db` and summaries
- `ts_archive` for detached/archived partitions

## Fill factor

- OLTP tables with frequent in-place updates (users, interviews, policies):
  `FILLFACTOR = 85` to reduce page splits.
- Append-only tables (`telemetry_events`, audit logs): `FILLFACTOR = 100`
  (default).
