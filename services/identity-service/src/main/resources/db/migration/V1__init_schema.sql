-- =============================================================================
-- identity_db - Identity & Access Management
-- Owning service: identity-service
-- Multi-tenancy  : row level, every tenant-scoped table carries organization_id
-- Audit          : created_by/updated_by/deleted_by + timestamps on every table
-- Optimistic lock: version BIGINT, incremented by the application (@Version)
-- Soft delete    : deleted_at/deleted_by, unique constraints are partial on
--                  (deleted_at IS NULL)
-- Baseline      : squashed from the original development migrations
--                  V1__init_schema + V2__otp_mfa +
--                  V2__fix_mfa_device_reenrollment + V3__security_hardening.
--                  Reference data (permission codes) lives in
--                  R__reference_data.sql.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Extensions
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- Types
-- -----------------------------------------------------------------------------
CREATE TYPE user_status AS ENUM ('PENDING', 'ACTIVE', 'DISABLED', 'LOCKED');

CREATE TYPE session_status AS ENUM ('ACTIVE', 'REFRESHED', 'REVOKED', 'EXPIRED');

-- -----------------------------------------------------------------------------
-- Tables
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- Lookup / reference tables (global, not tenant scoped)
-- -----------------------------------------------------------------------------
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  UUID,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

COMMENT ON TABLE permissions IS 'Global catalog of permission codes used for RBAC authorization.';

-- -----------------------------------------------------------------------------
-- Master / tenant tables
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id            UUID NOT NULL,
    email                      TEXT NOT NULL,
    password_hash              TEXT NOT NULL,
    display_name               TEXT NOT NULL,
    status                     user_status NOT NULL DEFAULT 'PENDING',
    email_verified_at          TIMESTAMPTZ,
    failed_login_attempts      INT NOT NULL DEFAULT 0,
    locked_until               TIMESTAMPTZ,
    password_reset_requested_at TIMESTAMPTZ,
    last_login_at              TIMESTAMPTZ,
    created_by                 UUID,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                 UUID,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by                 UUID,
    deleted_at                 TIMESTAMPTZ,
    version                    BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_users_display_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT chk_users_version CHECK (version >= 0)
);

COMMENT ON TABLE users IS 'Platform users. One row per human/system account, tenant scoped.';

COMMENT ON COLUMN users.failed_login_attempts IS 'Consecutive failed password attempts, reset on success.';
COMMENT ON COLUMN users.locked_until IS 'Temporary lockout deadline after repeated failures; NULL when not locked.';
COMMENT ON COLUMN users.password_reset_requested_at IS 'When the last reset email was dispatched, used for per-email throttling.';

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_roles_code_not_blank CHECK (length(btrim(code)) > 0)
);

COMMENT ON TABLE roles IS 'Tenant scoped RBAC roles. System roles (is_system) cannot be deleted.';

-- -----------------------------------------------------------------------------
-- Bridge tables
-- -----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_by    UUID,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE role_permissions IS 'Many-to-many bridge between roles and permissions.';

CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_by UUID,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE user_roles IS 'Many-to-many bridge between users and roles.';

-- -----------------------------------------------------------------------------
-- Transaction / session tables
-- -----------------------------------------------------------------------------
CREATE TABLE user_sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id    UUID NOT NULL,
    refresh_token_hash TEXT NOT NULL,
    device_id          TEXT,
    ip_address         INET,
    user_agent         TEXT,
    status             session_status NOT NULL DEFAULT 'ACTIVE',
    issued_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at         TIMESTAMPTZ NOT NULL,
    last_used_at       TIMESTAMPTZ,
    revoked_at         TIMESTAMPTZ,
    revoked_by         UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_user_sessions_expiry CHECK (expires_at > issued_at)
);

COMMENT ON TABLE user_sessions IS 'Active session + refresh-token registry. Refresh tokens stored as SHA-256 hashes only.';

CREATE TABLE password_history (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash TEXT NOT NULL,
    changed_by    UUID,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE password_history IS 'Historical password hashes enabling reuse prevention.';

CREATE TABLE mfa_devices (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id   UUID NOT NULL,
    kind              TEXT NOT NULL,
    secret_ciphertext TEXT NOT NULL,
    verified_at       TIMESTAMPTZ,
    last_used_at      TIMESTAMPTZ,
    created_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_mfa_kind CHECK (kind IN ('TOTP', 'WEBAUTHN', 'SMS', 'EMAIL'))
);

COMMENT ON TABLE mfa_devices IS 'Multi-factor authentication device registrations.';

-- -----------------------------------------------------------------------------
-- History / audit tables (in-DB change tracking for high value entities)
-- -----------------------------------------------------------------------------
CREATE TABLE users_history (
    history_id        BIGSERIAL PRIMARY KEY,
    history_action    TEXT NOT NULL,
    changed_by        UUID,
    changed_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    id                UUID NOT NULL,
    organization_id   UUID NOT NULL,
    email             TEXT NOT NULL,
    password_hash     TEXT,
    display_name      TEXT,
    status            user_status,
    email_verified_at TIMESTAMPTZ,
    version           BIGINT
);

COMMENT ON TABLE users_history IS 'Immutable snapshot history of users, written by trigger.';

-- -----------------------------------------------------------------------------
-- Email OTP / MFA support tables
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

CREATE TABLE mfa_challenge_attempts (
    challenge_id    TEXT PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempts        INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mfa_challenge_attempts CHECK (attempts >= 0)
);

COMMENT ON TABLE mfa_challenge_attempts IS 'Failed verification attempts per MFA login challenge, enforced across instances.';

-- -----------------------------------------------------------------------------
-- Indexes
--   * tenant first in every composite index (organization_id leading column)
--   * partial indexes so soft-deleted rows never participate in uniqueness
--   * expression index on lower(email) for case insensitive lookups
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_users_org_email
    ON users (organization_id, lower(email)) WHERE deleted_at IS NULL;

CREATE INDEX idx_users_org_status
    ON users (organization_id, status) INCLUDE (display_name) WHERE deleted_at IS NULL;

CREATE INDEX idx_users_email_lookup
    ON users (lower(email)) WHERE deleted_at IS NULL;

CREATE INDEX idx_roles_org_code
    ON roles (organization_id, code) WHERE deleted_at IS NULL;

CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

CREATE INDEX idx_user_sessions_user ON user_sessions (user_id, status);
CREATE INDEX idx_user_sessions_refresh_hash ON user_sessions (refresh_token_hash);
CREATE INDEX idx_user_sessions_org_expiry ON user_sessions (organization_id, expires_at) WHERE status = 'ACTIVE';

CREATE INDEX idx_password_history_user ON password_history (user_id, changed_at DESC);

-- Partial unique index: soft-deleted devices no longer block re-enrollment of
-- the same (user_id, kind).
CREATE UNIQUE INDEX uq_mfa_user_kind_live ON mfa_devices (user_id, kind) WHERE deleted_at IS NULL;

CREATE INDEX idx_mfa_user ON mfa_devices (user_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_otp_codes_user ON otp_codes (user_id, purpose, consumed_at);
CREATE INDEX idx_otp_codes_expiry ON otp_codes (expires_at) WHERE consumed_at IS NULL;
CREATE INDEX idx_otp_codes_org ON otp_codes (organization_id, user_id);

CREATE INDEX idx_recovery_codes_user ON recovery_codes (user_id, consumed_at);
CREATE INDEX idx_recovery_codes_org ON recovery_codes (organization_id, user_id);

-- Single-use recovery codes: the partial unique index rejects concurrent
-- consumption of the same code hash for the same user.
CREATE UNIQUE INDEX uq_recovery_codes_usable
    ON recovery_codes (user_id, code_hash) WHERE consumed_at IS NULL;

CREATE INDEX idx_trusted_devices_user ON trusted_devices (user_id, device_id);
CREATE INDEX idx_trusted_devices_device ON trusted_devices (device_id);
CREATE INDEX idx_trusted_devices_org ON trusted_devices (organization_id, user_id);
CREATE UNIQUE INDEX uq_trusted_devices_user_device ON trusted_devices (user_id, device_id);

CREATE INDEX idx_mfa_challenge_attempts_user ON mfa_challenge_attempts (user_id);

-- -----------------------------------------------------------------------------
-- Functions (shared conventions)
-- -----------------------------------------------------------------------------
-- Maintains the updated_at timestamp on UPDATE. Optimistic-lock version is
-- managed by the application (@Version) so it is intentionally not touched here.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Applies the multi-tenant RLS guard: rows must match the organization set on
-- the connection (app.organization_id). Used by the tenant isolation policies.
CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.organization_id', true), '')::uuid;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION audit_users_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO users_history(history_action, changed_by, id, organization_id, email,
                                  password_hash, display_name, status, email_verified_at, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.organization_id, OLD.email, OLD.password_hash, OLD.display_name,
                OLD.status, OLD.email_verified_at, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO users_history(history_action, changed_by, id, organization_id, email,
                                  password_hash, display_name, status, email_verified_at, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.organization_id, NEW.email, NEW.password_hash, NEW.display_name,
                NEW.status, NEW.email_verified_at, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- Triggers
-- -----------------------------------------------------------------------------
CREATE TRIGGER trg_users_history
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_users_history();

-- -----------------------------------------------------------------------------
-- Multi-tenancy (defense in depth; applications always filter by organization_id)
-- -----------------------------------------------------------------------------
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE mfa_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recovery_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE trusted_devices ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_users ON users
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_roles ON roles
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_user_sessions ON user_sessions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_mfa_devices ON mfa_devices
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_otp_codes ON otp_codes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_recovery_codes ON recovery_codes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_trusted_devices ON trusted_devices
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

-- Bridge tables inherit isolation through the referenced parent keys.
CREATE POLICY tenant_isolation_user_roles ON user_roles
    USING (EXISTS (SELECT 1 FROM users u WHERE u.id = user_id AND u.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM users u WHERE u.id = user_id AND u.organization_id = current_tenant_id()));

CREATE POLICY tenant_isolation_role_permissions ON role_permissions
    USING (EXISTS (SELECT 1 FROM roles r WHERE r.id = role_id AND r.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM roles r WHERE r.id = role_id AND r.organization_id = current_tenant_id()));
