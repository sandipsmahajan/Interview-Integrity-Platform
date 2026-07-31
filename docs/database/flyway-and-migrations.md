# Flyway & Migration Strategy

Each service ships its migrations in its own module:

```
services/<service>/src/main/resources/db/migration/
    V1__init_schema.sql      # versioned schema
    R__reference_data.sql    # repeatable seed/reference data (idempotent)
```

Flyway runs **versioned migrations (V*) in numeric order, then repeatable
migrations (R*)**. Each database is migrated by its owning service only; there
is no cross-database migration.

## Versioned migrations

- Naming: `V<n>__<snake_case_description>.sql`, monotonic per database.
- `V1__init_schema.sql` creates types, functions, tables, indexes, triggers,
  views, RLS, and calls partition-provisioning functions at the end.
- **Never edit an applied migration.** Schema evolution always adds a new
  versioned file. This preserves checksum integrity (`flyway validate`).

## Repeatable migrations

- Naming: `R__<name>.sql` (e.g. `R__reference_data.sql`).
- Re-run on every migration cycle **when their checksum changes**; all
  reference-data inserts are written as idempotent `INSERT ... SELECT ...
  WHERE NOT EXISTS` upserts.
- Examples: permission codes (identity), subscription plans (organization),
  telemetry event types (telemetry).

## Seed data

| Database | Seed | Type |
|----------|------|------|
| identity_db | 23 permission codes | reference data |
| organization_db | 3 subscription plans | reference data |
| telemetry_db | 17 event types with retention | reference data |

Seed data is reference data (stable, low cardinality). Environment-specific or
tenant data is never seeded by migrations; it is provisioned by services or
operator scripts.

## Rollback strategy

- **Forward-fix only** (Flyway Community/OSS model): bugs in an applied
  migration are repaired with a new versioned migration
  (`V2__fix_...sql`). There is no `undo`.
- **Checksum drift**: `flyway repair` re-aligns checksums after an
  intentional edit of a repeatable migration; versioned files are never
  edited.
- **Pre-production**: `flyway clean` + migrate rebuilds a database from
  scratch (targeted at local dev and CI only; never in production).
- **Production recovery**: if a migration fails, restore from the pre-migration
  backup (PITR) and re-apply. Partition drops/archives are idempotent and safe
  to re-run.

## Baseline

Fresh databases bootstrap directly from `V1__init_schema.sql`. If a database
already contains schema created outside Flyway, set a baseline
(`flyway baseline -baselineVersion=1`) so Flyway does not re-run `V1`.

## Partition provisioning

Partitioned tables (telemetry, audit) call their `*_ensure_partitions()`
functions at the end of the migration and again from scheduler jobs, so new
months are always pre-created before the first insert arrives.

## Migration safety practices

- Versioned migrations run in a transaction (Flyway default for PostgreSQL);
  partition DDL is transactional in PostgreSQL 15+, so a failed migration
  rolls back cleanly.
- Large index additions in high-volume tables use
  `CREATE INDEX CONCURRENTLY` in dedicated versioned migrations (skips the
  table lock).
- `ALTER TYPE ... ADD VALUE` runs in its own migration and cannot run inside a
  transaction block - keep it isolated from other DDL.
- Every migration is validated against a real PostgreSQL instance (see
  `docs/database/README.md`) before merge.

## CI integration

1. `./gradlew :services:<service>:bootRun` applies migrations at startup
   (`spring.flyway.enabled=true`), or
2. `flyway migrate` runs as a deployment step before the new service version
   starts (recommended for large partitions and long-running DDL).
