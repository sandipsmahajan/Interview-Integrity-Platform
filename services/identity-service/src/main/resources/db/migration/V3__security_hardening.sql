-- =============================================================================
-- identity_db - Security hardening
-- Owning service: identity-service
-- Adds account lockout, MFA challenge attempt limiting, tenant index coverage
-- and atomic single-use guarantees for MFA recovery codes and trusted devices.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Account lockout
-- -----------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS password_reset_requested_at TIMESTAMPTZ;

COMMENT ON COLUMN users.failed_login_attempts IS 'Consecutive failed password attempts, reset on success.';
COMMENT ON COLUMN users.locked_until IS 'Temporary lockout deadline after repeated failures; NULL when not locked.';
COMMENT ON COLUMN users.password_reset_requested_at IS 'When the last reset email was dispatched, used for per-email throttling.';

-- -----------------------------------------------------------------------------
-- MFA challenge attempt limiting (distributed brute-force guard)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mfa_challenge_attempts (
    challenge_id    TEXT PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempts        INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mfa_challenge_attempts CHECK (attempts >= 0)
);

COMMENT ON TABLE mfa_challenge_attempts IS 'Failed verification attempts per MFA login challenge, enforced across instances.';

CREATE INDEX IF NOT EXISTS idx_mfa_challenge_attempts_user ON mfa_challenge_attempts (user_id);

-- -----------------------------------------------------------------------------
-- Tenant index coverage (RLS policy scans filter on organization_id first)
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_otp_codes_org ON otp_codes (organization_id, user_id);
CREATE INDEX IF NOT EXISTS idx_recovery_codes_org ON recovery_codes (organization_id, user_id);
CREATE INDEX IF NOT EXISTS idx_trusted_devices_org ON trusted_devices (organization_id, user_id);

-- -----------------------------------------------------------------------------
-- Single-use recovery codes: guard against concurrent consumption
-- -----------------------------------------------------------------------------
-- A recovery code is single-use; the partial unique index rejects concurrent
-- consumption of the same code hash for the same user.
DROP INDEX IF EXISTS uq_recovery_codes_usable;
CREATE UNIQUE INDEX uq_recovery_codes_usable
    ON recovery_codes (user_id, code_hash) WHERE consumed_at IS NULL;

-- -----------------------------------------------------------------------------
-- Trusted devices: one row per (user, device)
-- -----------------------------------------------------------------------------
-- Existing duplicate rows would violate the unique constraint; deduplicate by
-- keeping the most recently seen row before installing the constraint.
DELETE FROM trusted_devices a
    USING trusted_devices b
    WHERE a.user_id = b.user_id
      AND a.device_id = b.device_id
      AND a.last_seen_at < b.last_seen_at;

DROP INDEX IF EXISTS uq_trusted_devices_user_device;
CREATE UNIQUE INDEX uq_trusted_devices_user_device ON trusted_devices (user_id, device_id);
