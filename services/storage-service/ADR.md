# Architecture Decision Records - storage-service

## ADR-0001: Service ownership

The storage service (object metadata, signed URLs) owns the domain described by its schema. All mutations go through
this service; consumers read via its REST API or subscribe to its Kafka events.

## ADR-0002: Data isolation

The service owns storage_db exclusively. Cross-database references use soft UUID
columns; referential integrity across services is eventual, enforced by
Kafka events.

## ADR-0003: Multi-tenancy

Every tenant-scoped table carries `organization_id` and is protected by row
level security. The service reads the tenant from the authenticated context
and never trusts tenant id values in the request body.
