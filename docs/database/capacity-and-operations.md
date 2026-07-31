# Capacity Planning & Operations

## Row-size model

Approximate physical row sizes (including TOAST for TEXT/JSONB):

| Table | Bytes/row | Notes |
|-------|-----------|-------|
| `users` | ~250 | mostly short text + uuid |
| `candidates` | ~300 | + `candidate_profiles` ~200 |
| `interviews` | ~350 | + `interview_sessions` ~200 |
| `violations` | ~400 | + evidence JSONB |
| `telemetry_events` | ~600 | compact JSONB payload |
| `audit_events` | ~450 | + metadata JSONB |
| `api_audit_log` | ~200 | |
| daily summaries | ~150 | wide counter row |

## Storage estimates

### OLTP databases (per 1M rows)

| Database | 1M rows | 10M rows | Drivers |
|----------|---------|----------|---------|
| identity_db | 0.6 GB | 5 GB | users, sessions |
| organization_db | 0.3 GB | 2 GB | small footprint |
| recruiter_db | 0.5 GB | 4 GB | pipeline rows |
| candidate_db | 1.2 GB | 10 GB | profiles, documents, notes |
| interview_db | 1.0 GB | 8 GB | interviews + feedback |
| policy_db | 0.8 GB | 6 GB | violations dominate |
| notification_db | 0.6 GB | 5 GB | + deliveries |
| report_db | 0.4 GB | 3 GB | metadata only (artifacts in object store) |
| storage_db | 0.4 GB | 3 GB | metadata only |
| feature_flag_db | 0.2 GB | 1.5 GB | |
| scheduler_db | 0.3 GB | 2.5 GB | executions accumulate |
| integration_db | 0.3 GB | 2.5 GB | sync logs |
| configuration_db | 0.1 GB | 0.8 GB | |

Indexes add roughly 30-50% on top of these figures for OLTP tables.

### Telemetry (the dominant cost)

At ~600 bytes/row plus indexes (BRIN negligible, GIN ~30%, covering ~35%,
org_time ~25%) a telemetry row costs ~1.1 KB all-in.

| Scenario | Events/day | GB/day | 12 months | 24 months |
|----------|-----------|--------|-----------|-----------|
| Small (200 interviews/day) | 0.6M | 0.7 | 0.25 TB | 0.5 TB |
| Mid (2k interviews/day) | 6M | 6.6 | 2.4 TB | 4.8 TB |
| Large (20k interviews/day) | 60M | 66 | 24 TB | 48 TB |

Retention policy (per event type, 30-730 days) caps the growth; the largest
reduction comes from dropping heartbeat/key event partitions after their
retention window.

### Analytics

Daily summaries grow ~5 MB/day/tenant in the worst case; monthly matviews are
a fraction of that. Analytics storage is negligible against telemetry.

## Capacity planning model

```
telemetry_disk = avg_rows_per_interview
               x interviews_per_day
               x 1.1 KB
               x retention_days
               x replication_factor
```

Recommended headroom: provision **2.5x** the projected steady-state size to
absorb growth, reindexing windows, and WAL replay during recovery.

## Growth strategy

- **Vertical first**: current telemetry partitions on NVMe; tier older
  partitions to SSD then HDD/object storage via tablespace moves and
  detach+archive.
- **Read replicas**: route dashboard/reporting traffic (analytics_db, reports,
  telemetry rollups) to read replicas; primary serves writes.
- **Sharding exit ramp**: `organization_id` in every key makes hash sharding or
  per-tenant clusters a data-movement exercise if one cluster becomes
  insufficient. The tenant root guarantees referential integrity within a
  shard.
- **Partition tuning**: if a monthly partition exceeds ~300 GB, move
  `telemetry_events` to weekly grain (partition creation is parameterized).
- **Autovacuum**: `telemetry_events` is append-only, so autovacuum work is
  minimal; tune `autovacuum_vacuum_scale_factor` to 0.01 on hot OLTP tables
  with frequent updates (users, interviews, policies, notifications).

## Backup strategy

Tooling: **pgBackRest** with WAL archiving and point-in-time recovery.

| Tier | Scope | Frequency | Retention | RPO | RTO |
|------|-------|-----------|-----------|-----|-----|
| Differential | all databases | daily | 7 days | 24h+WAL | 1h |
| Full | all databases | weekly | 4 weeks | - | 2h |
| Archive full | all databases | monthly | 12 months | - | 4h |
| WAL | archived continuously | continuous | 14 days | 5 min | minutes |

- `telemetry_events` partitions older than 90 days are excluded from daily
  differentials (they are immutable; restored by archive + partition replay).
- Object-store backups for archived telemetry partitions and report artifacts
  (S3/MinIO versioning + cross-region replication).
- Backups are encrypted at rest and periodically restore-tested in a staging
  cluster.

## Disaster recovery

| Scenario | Strategy |
|----------|----------|
| Single node loss | Primary + synchronous/async replica per database; failover via HA tooling |
| Database corruption | PITR to just-before-corruption from pgBackRest |
| Region loss | Cross-region backups + archived partitions; RTO 4h |
| Ransomware / bad migration | Monthly archive fulls immutable (WORM) |
| Data breach | audit_events (7y) + partition archival retained independently |

### Replication

- Per-database standby replicas (all OLTP DBs) using streaming replication.
- `analytics_db` and `telemetry_db` run additional read replicas for reporting
  load.
- Monitoring: `pg_stat_replication` lag alerts, `pg_stat_activity` long query
  alerts, partition coverage alerts (`telemetry_ensure_partitions` drift).

## Connection & pooling

- One connection pool per service per database, sized
  `max_connections = cores * 2 + spindle_heads`; R2DBC pool (`r2dbc-pool`)
  defaults tuned per service.
- Every pooled connection runs `SET app.organization_id` / `app.user_id` at
  checkout (transaction-scoped with `SET LOCAL` where possible).
