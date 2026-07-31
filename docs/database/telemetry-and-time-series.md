# Telemetry & Time-Series Design

`telemetry_events` is the platform's largest table (billions of rows). It is
designed as a **write-optimized, partition-managed, time-series store** inside
standard PostgreSQL.

## Workload profile

| Property | Target |
|----------|--------|
| Ingest rate | up to 50k events/sec across the fleet |
| Row size | ~600 bytes (compact JSONB payload) |
| Query profile | session replay (by `session_id`), time-window analytics (by `occurred_at`), tenant dashboards |
| Retention | raw events by event type (30-730 days), rollups permanent |

## Partitioning

`telemetry_events` is `PARTITION BY RANGE (occurred_at)` at **monthly** grain:

```sql
CREATE TABLE telemetry_events (
    id UUID NOT NULL,
    organization_id UUID NOT NULL,
    session_id UUID NOT NULL,
    interview_id UUID,
    event_type TEXT NOT NULL,
    seq BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    client_occurred_at TIMESTAMPTZ,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);
```

Monthly partitions balance partition count against per-partition size
(roughly 100-300 GB/month at target volume - large enough to amortize seq
scans, small enough to detach/drop quickly).

Lifecycle functions:

| Function | Purpose |
|----------|---------|
| `telemetry_create_partition(month)` | creates one partition + its indexes, analyzes it |
| `telemetry_ensure_partitions(n)` | pre-creates the next n months (called at startup and by scheduler) |
| `telemetry_archive_partition(month)` | `DETACH PARTITION` for cold archival |
| `telemetry_drop_partition_before(month)` | drops partitions older than a month (retention) |

## Retention

- Retention is per `telemetry_event_types.retention_days` (e.g. heartbeats
  180 days, key-stroke metadata 90 days, tab-switch/focus events 730 days).
- The scheduler-service invokes `telemetry_drop_partition_before()` based on
  the configured policy; a partition is dropped only when its month is older
  than every event type's retention.
- Rolling the partition boundary forward makes bulk expiry a metadata
  operation (`DROP TABLE`), not `DELETE`.

## Compression & storage

- **BRIN index** on `(occurred_at, seq)` with `pages_per_range = 128` gives
  range queries a compact per-8MB-page bitmap (index is ~1 MB vs ~20+ GB of
  B-tree at scale) with negligible insert cost.
- **TOAST** compresses JSONB payloads automatically (JSON compresses well;
  typical 2-4x).
- For production: `FILLFACTOR = 100` (append-only) and dedicated tablespaces
  so current-month partitions sit on NVMe while old partitions tier to HDD/object
  storage via detach.

## Aggregation tables (hourly rollups)

`telemetry_event_summaries` is a monthly-partitioned, per
`(bucket_start, organization_id, session_id, event_type)` rollup:

- `telemetry_rollup_hour(bucket)` is an idempotent upsert that aggregates one
  hour of raw events (`count`, `min/max seq`, last payload).
- Rollups are produced by scheduler-service (or `pg_cron`) shortly after each
  hour closes, and again as a repair pass over late-arriving events.
- Dashboard time-window queries read rollups; raw rows are reserved for
  session replay and deep audit.

## Materialized view

`mv_telemetry_event_type_stats` (cross-tenant event-type statistics) is a
`MATERIALIZED VIEW ... WITH NO DATA` refreshed by scheduler-service; refresh
frequency is configurable (default hourly).

## Archival

- `telemetry_archive_partition(month)` detaches the partition (instant, no row
  copy) so the app keeps writing to newer partitions.
- The detached table is exported (e.g. `pg_dump -t` or AWS S3 via
  `pg_dump`/`COPY`) and the local copy is dropped.
- Rehydration of an archived session reads from cold storage on demand.

## Query patterns

| Pattern | Reads |
|---------|-------|
| Session replay | covering index `(session_id, event_type, seq) INCLUDE (payload)` |
| Tenant time window | `(organization_id, occurred_at)` per partition |
| Event-type breakdown | `mv_telemetry_event_type_stats` |
| Dashboard (hour/day/week) | `telemetry_event_summaries` + analytics_db summaries |
| Compliance drill-down | partition-level scan with BRIN |
