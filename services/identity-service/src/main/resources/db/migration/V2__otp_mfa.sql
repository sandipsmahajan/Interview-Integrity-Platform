-- =============================================================================
-- identity_db - Email OTP, MFA recovery codes and trusted devices
-- Owning service: identity-service
-- Follows the V1 conventions: soft delete, optimistic lock, RLS isolation.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Email one-time passcodes
-- -----------------------------------------------------------------------------
CREATE TABLE otp_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    purpose         TEXT NOT NULL,
    code_hash       TEXT NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 5,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_otp_code_hash_not_blank CHECK (length(btrim(code_hash)) > 0),
    CONSTRAINT chk_otp_purpose_not_blank CHECK (length(btrim(purpose)) > 0),
    CONSTRAINT chk_otp_max_attempts CHECK (max_attempts > 0)
);

COMMENT ON TABLE otp_codes IS 'Short-lived one-time passcodes delivered by email. Stored as SHA-256 hashes only.';

CREATE INDEX idx_otp_codes_user ON otp_codes (user_id, purpose, consumed_at);
CREATE INDEX idx_otp_codes_expiry ON otp_codes (expires_at) WHERE consumed_at IS NULL;

ALTER TABLE otp_codes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_otp_codes ON otp_codes;
CREATE POLICY tenant_isolation_otp_codes ON otp_codes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

-- -----------------------------------------------------------------------------
-- MFA recovery codes (single use)
-- -----------------------------------------------------------------------------
CREATE TABLE recovery_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    code_hash       TEXT NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_recovery_code_hash_not_blank CHECK (length(btrim(code_hash)) > 0)
);

COMMENT ON TABLE recovery_codes IS 'Single-use backup codes for MFA recovery. Stored as SHA-256 hashes only.';

CREATE INDEX idx_recovery_codes_user ON recovery_codes (user_id, consumed_at);

ALTER TABLE recovery_codes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_recovery_codes ON recovery_codes;
CREATE POLICY tenant_isolation_recovery_codes ON recovery_codes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

-- -----------------------------------------------------------------------------
-- Trusted MFA devices (skip the challenge on known devices)
-- -----------------------------------------------------------------------------
CREATE TABLE trusted_devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    device_id       TEXT NOT NULL,
    device_name     TEXT,
    first_seen_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trusted_device_id_not_blank CHECK (length(btrim(device_id)) > 0)
);

COMMENT ON TABLE trusted_devices IS 'Devices exempted from MFA challenges after a successful verification.';

CREATE INDEX idx_trusted_devices_user ON trusted_devices (user_id, device_id);
CREATE INDEX idx_trusted_devices_device ON trusted_devices (device_id);

ALTER TABLE trusted_devices ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_trusted_devices ON trusted_devices;
CREATE POLICY tenant_isolation_trusted_devices ON trusted_devices
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
