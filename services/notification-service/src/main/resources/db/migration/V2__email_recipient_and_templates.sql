-- =============================================================================
-- notification_db - V2: email recipient
-- Owning service: notification-service
-- Platform default email templates moved to R__reference_data.sql (repeatable).
-- =============================================================================

-- The recipient address is persisted so the retry worker can re-dispatch a
-- pending email without re-reading the source event.
ALTER TABLE notifications ADD COLUMN recipient TEXT;

COMMENT ON COLUMN notifications.recipient IS 'Recipient address for email dispatch; null for non-email channels.';

CREATE INDEX idx_notifications_email_pending
    ON notifications (channel, status, created_at)
    WHERE channel = 'EMAIL' AND status = 'PENDING';
