# Architecture Decision Records - notification-service

## ADR-0001: Service ownership

The notification service (channels, templates, deliveries) owns the domain described by its schema.
All mutations go through
this service; consumers read via its REST API or subscribe to its Kafka events.

## ADR-0002: Data isolation

The service owns notification_db exclusively. Cross-database references use soft UUID
columns; referential integrity across services is eventual, enforced by
Kafka events.

## ADR-0003: Multi-tenancy

Every tenant-scoped table carries `organization_id` and is protected by row
level security. The service reads the tenant from the authenticated context
and never trusts tenant id values in the request body.

## ADR-0004: User-scoped self-service reads

Notification reads used by the portal (`list`, `get`, `markRead`, `deliveries`) resolve the caller
from the authenticated principal and reject requests that target another user's notification
(401/403). Provider outcome callbacks (`/sent`, `/delivered`, `/failed`) remain org-scoped because
the email provider is not a user.

## ADR-0005: Idempotent event-driven notifications

Notifications created from `identity.email.v1` events carry the source `EventEnvelope.eventId` in
`source_event_id` with a partial unique index. A Kafka redelivery cannot create a duplicate email
notification; API-created notifications keep the column null and are unconstrained.

## ADR-0006: At-least-once dispatch with single-winner release

Workers claim notifications for dispatch with an atomic conditional update. Only the worker that won
the claim releases the lease; a losing worker's release is a no-op, and a crashed worker's lease
expires on its own. A notification with zero recorded attempts is always considered due so it cannot
be stranded in `PENDING`.

