-- =============================================================================
-- notification_db - V3: dispatch claim + SMTP delivery hardening
-- Owning service: notification-service
-- =============================================================================

-- An atomic dispatch claim. A worker claims a pending notification before
-- sending so concurrent instances (or the retry worker racing the consumer)
-- cannot dispatch the same email twice. The claim is released once the send
-- attempt completes.
ALTER TABLE notifications ADD COLUMN claimed_at TIMESTAMPTZ;

COMMENT ON COLUMN notifications.claimed_at IS 'Timestamp of the in-flight dispatch claim; NULL when not claimed.';

CREATE INDEX idx_notifications_claim
    ON notifications (id, status)
    WHERE claimed_at IS NULL;
