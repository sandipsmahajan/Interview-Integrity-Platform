# Architecture Decision Records - telemetry-service

## ADR-0001: Service ownership

The telemetry service (event ingestion, rollups, retention) owns the domain described by its schema.
All mutations go through
this service; consumers read via its REST API or subscribe to its Kafka events.

## ADR-0002: Data isolation

The service owns telemetry_db exclusively. Cross-database references use soft UUID
columns; referential integrity across services is eventual, enforced by
Kafka events.

## ADR-0003: Multi-tenancy

Every tenant-scoped table carries `organization_id` and is protected by row
level security. The service reads the tenant from the authenticated context
and never trusts tenant id values in the request body.

## ADR-0004: Manual offset commit with retry and dead-letter routing

The consumer disables Kafka auto-commit so an offset only advances on an explicit acknowledge
after the batch is persisted. Transient processing failures are retried with bounded exponential
backoff; records that exhaust retries are published to `telemetry.received.dlq.v1` and the source
offset is acknowledged, so a poison message cannot stall the partition.

