# Database Architecture

Production-grade PostgreSQL design for the Interview Integrity Platform's
microservices. Every service owns one database; **no database is shared**.

## Database inventory

| Database          | Owning service       | Domain                                    | Migration path |
|-------------------|----------------------|-------------------------------------------|----------------|
| `identity_db`     | identity-service     | Users, roles, permissions, sessions, MFA  | `services/identity-service/src/main/resources/db/migration` |
| `organization_db` | organization-service | Tenants, plans, subscriptions, hierarchy  | `services/organization-service/src/main/resources/db/migration` |
| `recruiter_db`    | recruiter-service    | Recruiter profiles, pipeline              | `services/recruiter-service/src/main/resources/db/migration` |
| `candidate_db`    | candidate-service    | Candidate profiles, documents, assessments| `services/candidate-service/src/main/resources/db/migration` |
| `interview_db`    | interview-service    | Interviews, sessions, panels, feedback    | `services/interview-service/src/main/resources/db/migration` |
| `telemetry_db`    | telemetry-service    | Time-series telemetry (billions of rows)  | `services/telemetry-service/src/main/resources/db/migration` |
| `policy_db`       | policy-engine-service| Policies, rules, violations               | `services/policy-engine-service/src/main/resources/db/migration` |
| `report_db`       | report-service       | Reports, sections, schedules              | `services/report-service/src/main/resources/db/migration` |
| `notification_db` | notification-service | Notifications, templates, deliveries      | `services/notification-service/src/main/resources/db/migration` |
| `analytics_db`    | analytics-service    | Daily/weekly/monthly summaries            | `services/analytics-service/src/main/resources/db/migration` |
| `audit_db`        | audit-service        | Compliance audit + API access log         | `services/audit-service/src/main/resources/db/migration` |
| `storage_db`      | storage-service      | Object storage metadata, signed URLs      | `services/storage-service/src/main/resources/db/migration` |
| `feature_flag_db` | feature-flag-service | Features, flags, targets, experiments     | `services/feature-flag-service/src/main/resources/db/migration` |
| `scheduler_db`    | scheduler-service    | Jobs, executions, distributed locks       | `services/scheduler-service/src/main/resources/db/migration` |
| `integration_db`  | integration-service  | Integrations, connections, webhooks       | `services/integration-service/src/main/resources/db/migration` |
| `configuration_db`| configuration-service| Versioned key/value configuration         | `services/configuration-service/src/main/resources/db/migration` |

`api-gateway` and `discovery-service` are stateless and own no database.

## Key facts

- **Tenant isolation**: `organizations` in `organization_db` is the tenant root.
  Every tenant-scoped table in every database carries `organization_id` and
  has Row Level Security enabled. Details in `multi-tenancy.md`.
- **Identity**: `users.id` in `identity_db` is the platform-wide actor
  identifier; all other databases reference it as a **soft reference**
  (UUID column, no cross-database FK).
- **Normalization**: 3NF everywhere; denormalization happens only in the
  analytics summaries (documented in `analytics-and-aggregations.md`).
- **Telemetry**: `telemetry_events` is partitioned by month and expected to
  reach billions of rows. Details in `telemetry-and-time-series.md`.

## Documentation index

- `naming-standards.md` - naming, types, audit columns, conventions
- `logical-model.md` - entities, relationships, cardinality per database
- `physical-model.md` - physical storage decisions, ENUMs, JSONB, sequences
- `er-diagrams.md` - Mermaid ER diagrams per database
- `multi-tenancy.md` - tenant isolation strategy and RLS policies
- `audit-strategy.md` - in-DB history tables + central audit trail
- `index-strategy.md` - B-tree, GIN, BRIN, partial, expression, covering
- `telemetry-and-time-series.md` - partitioning, retention, archival, rollups
- `analytics-and-aggregations.md` - summaries and aggregation jobs
- `capacity-and-operations.md` - storage estimates, growth, backup, DR
- `flyway-and-migrations.md` - migration, seed, and rollback strategy

## Validation

All migrations have been applied to and validated against a real PostgreSQL
15 instance (fresh databases, `V*` before `R*`, `ON_ERROR_STOP=1`).
