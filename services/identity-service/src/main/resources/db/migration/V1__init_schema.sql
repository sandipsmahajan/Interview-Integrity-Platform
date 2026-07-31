-- =============================================================================
-- identity_db - Identity & Access Management
-- Owning service: identity-service
-- Multi-tenancy  : row level, every tenant-scoped table carries organization_id
-- Audit          : created_by/updated_by/deleted_by + timestamps on every table
-- Optimistic lock: version BIGINT, incremented by the application (@Version)
-- Soft delete    : deleted_at/deleted_by, unique constraints are partial on
--                  (deleted_at IS NULL)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Extensions
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- ENUM types
-- -----------------------------------------------------------------------------
CREATE TYPE user_status AS ENUM ('PENDING', 'ACTIVE', 'DISABLED', 'LOCKED');

CREATE TYPE session_status AS ENUM ('ACTIVE', 'REFRESHED', 'REVOKED', 'EXPIRED');

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
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL,
    email             TEXT NOT NULL,
    password_hash     TEXT NOT NULL,
    display_name      TEXT NOT NULL,
    status            user_status NOT NULL DEFAULT 'PENDING',
    email_verified_at TIMESTAMPTZ,
    last_login_at     TIMESTAMPTZ,
    created_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_users_display_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT chk_users_version CHECK (version >= 0)
);

COMMENT ON TABLE users IS 'Platform users. One row per human/system account, tenant scoped.';

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
-- Transaction / history tables
-- -----------------------------------------------------------------------------
CREATE TABLE user_sessions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id   UUID NOT NULL,
    refresh_token_hash TEXT NOT NULL,
    device_id         TEXT,
    ip_address        INET,
    user_agent        TEXT,
    status            session_status NOT NULL DEFAULT 'ACTIVE',
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at        TIMESTAMPTZ NOT NULL,
    last_used_at      TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    revoked_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 1,
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
    CONSTRAINT chk_mfa_kind CHECK (kind IN ('TOTP', 'WEBAUTHN', 'SMS', 'EMAIL')),
    CONSTRAINT uq_mfa_user_kind UNIQUE (user_id, kind)
);

COMMENT ON TABLE mfa_devices IS 'Multi-factor authentication device registrations.';

-- -----------------------------------------------------------------------------
-- History / audit tables (in-DB change tracking for high value entities)
-- -----------------------------------------------------------------------------
CREATE TABLE users_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    email           TEXT NOT NULL,
    password_hash   TEXT,
    display_name    TEXT,
    status          user_status,
    email_verified_at TIMESTAMPTZ,
    version         BIGINT
);

COMMENT ON TABLE users_history IS 'Immutable snapshot history of users, written by trigger.';

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

CREATE TRIGGER trg_users_history
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_users_history();

-- -----------------------------------------------------------------------------
-- Index strategy
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

CREATE INDEX idx_mfa_user ON mfa_devices (user_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Multi-tenancy (defense in depth; applications always filter by organization_id)
-- -----------------------------------------------------------------------------
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE mfa_devices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_users ON users;
CREATE POLICY tenant_isolation_users ON users
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_roles ON roles;
CREATE POLICY tenant_isolation_roles ON roles
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_user_sessions ON user_sessions;
CREATE POLICY tenant_isolation_user_sessions ON user_sessions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

DROP POLICY IF EXISTS tenant_isolation_mfa_devices ON mfa_devices;
CREATE POLICY tenant_isolation_mfa_devices ON mfa_devices
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

-- Bridge tables inherit isolation through the referenced parent keys.
DROP POLICY IF EXISTS tenant_isolation_user_roles ON user_roles;
CREATE POLICY tenant_isolation_user_roles ON user_roles
    USING (EXISTS (SELECT 1 FROM users u WHERE u.id = user_id AND u.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM users u WHERE u.id = user_id AND u.organization_id = current_tenant_id()));

DROP POLICY IF EXISTS tenant_isolation_role_permissions ON role_permissions;
CREATE POLICY tenant_isolation_role_permissions ON role_permissions
    USING (EXISTS (SELECT 1 FROM roles r WHERE r.id = role_id AND r.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM roles r WHERE r.id = role_id AND r.organization_id = current_tenant_id()));
