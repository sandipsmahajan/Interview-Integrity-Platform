-- Adds consumer idempotency for the identity.email.v1 event stream.
--
-- IdentityEmailConsumer creates one notification per consumed event. When a
-- record is redelivered (consumer restart before ack, duplicate in the log),
-- the same event would previously create a duplicate notification and email.
--
-- The source_event_id column records the originating EventEnvelope.eventId and
-- the partial unique index lets the consumer deduplicate on (re)delivery while
-- keeping API-created notifications (which have no source event) unconstrained.

ALTER TABLE notifications ADD COLUMN source_event_id UUID;

COMMENT ON COLUMN notifications.source_event_id IS 'EventEnvelope.eventId that produced this notification; NULL for API-created notifications.';

CREATE UNIQUE INDEX uq_notifications_source_event
    ON notifications (source_event_id)
    WHERE source_event_id IS NOT NULL;
