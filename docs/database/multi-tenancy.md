# Multi-Tenancy

## Model

**Shared-schema, row-level isolation.** All tenants share one schema per
database; every tenant-scoped table carries an `organization_id UUID NOT NULL`
column. `organizations.id` in `organization_db` is the single source of truth
for tenant identity, and every other database stores the same UUID.

This model maximizes operational simplicity (one cluster, standard backup
tooling) while guaranteeing data isolation through three reinforcing layers:

## Layer 1 - Application-level isolation (primary)

Every query issued by a service is scoped by `organization_id`:

- Spring Data R2DBC repositories derive it from the authenticated principal
  (`AuthenticatedUser.organizationId()`) and always include it in WHERE and
  INSERT clauses.
- No repository method is permitted to read or write without a tenant context.
  This is enforced by code review and the shared repository base classes.

## Layer 2 - Row Level Security (defense in depth)

Every tenant-scoped table enables RLS and installs a policy:

```sql
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_users ON users
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
```

`current_tenant_id()` reads the `app.organization_id` connection setting:

```sql
CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.organization_id', true), '')::uuid;
END;
$$ LANGUAGE plpgsql STABLE;
```

Operational contract:

- The connection pool runs
  `SET app.organization_id = '<uuid>'` (and `SET app.user_id = '<uuid>'`) as
  the first statement on every pooled connection, per request.
- `SET LOCAL` inside a transaction is recommended so the setting never leaks
  between requests on a pooled connection.
- If the setting is missing (`NULL`), the policy matches no rows, so a
  misconfigured connection fails closed rather than leaking data.
- Global reference tables (`permissions`, `plans`, `telemetry_event_types`,
  `configuration_schema`, `notification_templates`) have no tenant policy.
- SYSTEM-scoped configuration is readable by all tenants via a dedicated
  policy (`scope = 'SYSTEM' OR organization_id = current_tenant_id()`).
- Bridge tables inherit isolation through the referenced parent (e.g.
  `user_roles` is isolated by checking the user's organization).

## Layer 3 - Physical separation (optional, per customer)

For customers with contractual isolation requirements, the same migrations can
be applied to a dedicated database or cluster. Because tenants are identified
by `organization_id` everywhere, moving a tenant is a data-migration exercise,
not a schema change.

## Cross-database references (soft references)

Cross-service ownership means no foreign keys between databases. The referenced
identifier is stored as a plain UUID:

| Reference | Source column | Owning table (other DB) |
|-----------|---------------|-------------------------|
| tenant | `organization_id` | `organizations` (organization_db) |
| actor | `created_by`/`updated_by`/`user_id` | `users` (identity_db) |
| candidate | `candidate_id` | `candidates` (candidate_db) |
| recruiter | `recruiter_id` | `recruiters` (recruiter_db) |
| interview session | `session_id`/`interview_id` | `interviews`/`interview_sessions` (interview_db) |
| stored object | `storage_object_id` | `storage_objects` (storage_db) |

Referential integrity across services is achieved **eventually** through Kafka
domain events (e.g. `interview.created.v1`), not through FK constraints.

## Role and permission model (RBAC)

- `permissions` (global catalog) N:M `roles` via `role_permissions`.
- `users` N:M `roles` via `user_roles`.
- Services authorize with the permission codes declared in
  `R__reference_data.sql` (identity_db) and JWT roles/permissions issued by
  identity-service.
