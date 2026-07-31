# Naming Standards

Applies to every table, column, type, index, constraint, function and trigger
in all sixteen databases.

## Tables and columns

| Rule | Standard | Example |
|------|----------|---------|
| Identifiers | `snake_case`, lower case | `interview_sessions`, `violations` |
| Table names | plural nouns | `users`, `storage_objects` |
| Primary key | `id UUID` | `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` |
| Foreign key | `<referenced_table_singular>_id` | `organization_id`, `candidate_id` |
| Bridge tables | concatenation of the two sides | `role_permissions`, `user_roles`, `interview_panels` |
| Boolean | `is_` / `has_` prefix | `is_system`, `email_verified_at` |
| Timestamp | `_at` suffix, `TIMESTAMPTZ` | `created_at`, `occurred_at` |
| JSONB | meaningful noun | `payload`, `metadata`, `condition`, `variants` |
| Arrays | plural noun | `specialties`, `scopes`, `events` |
| Counters | `_count` suffix | `usage_count`, `attempts` |
| Money | `_cents` suffix, `BIGINT` | `monthly_price_cents` |

## Audit columns (every tenant-scoped table)

| Column | Type | Purpose |
|--------|------|---------|
| `created_by` | `UUID` | actor that created the row |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | creation instant |
| `updated_by` | `UUID` | actor that last updated the row |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | last update instant (maintained by trigger) |
| `deleted_by` | `UUID` | actor that soft-deleted the row |
| `deleted_at` | `TIMESTAMPTZ` | soft-delete marker (`NULL` = live) |
| `version` | `BIGINT NOT NULL DEFAULT 1` | optimistic-lock counter (application-managed `@Version`) |

Lookup/reference tables that are never updated keep only `created_at` /
`updated_at` / `version`. History tables use `history_id BIGSERIAL`, a
`history_action TEXT` (`INSERT`/`UPDATE`/`DELETE`) and `changed_at`.

## ENUM types

- Named `snake_case`, singular, suffixed with the subject, e.g. `user_status`,
  `interview_status`, `violation_severity`.
- Declared with `CREATE TYPE ... AS ENUM`. Adding values later requires a
  single new migration `ALTER TYPE ... ADD VALUE` (no rewrite).

## Constraints

- Check constraints are named `chk_<table>_<rule>`.
- Foreign keys rely on PostgreSQL's default auto-generated names.
- Unique constraints are `uq_<table>_<columns>`.
- Non-unique indexes are `idx_<table>_<columns>`.
- Partition-local indexes get the suffix `_<partition>` appended
  (`telemetry_events_2026_08_occurred_brin`).

## Soft-delete aware uniqueness

Unique business keys are **not** declared as `UNIQUE` constraints on the
column. They are partial unique indexes:

```sql
CREATE UNIQUE INDEX uq_users_org_email
    ON users (organization_id, lower(email)) WHERE deleted_at IS NULL;
```

This lets a tenant delete a user and later recreate the same email.

## Functions and triggers

- Shared maintenance functions are named `set_updated_at()` and
  `current_tenant_id()`. They are created idempotently in every database
  (`CREATE OR REPLACE`).
- History triggers are `trg_<table>_history` calling `audit_<table>_history()`.
- Partition-management functions are prefixed with the domain:
  `telemetry_*`, `audit_*`, `analytics_*`.
