-- =============================================================================
-- configuration_db - Key/value configuration with schema and history
-- Owning service: configuration-service
-- Configuration is tenant scoped, versioned, and validated against a global
-- schema catalog.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE config_value_type AS ENUM ('STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'DURATION');

CREATE TYPE config_scope AS ENUM ('SYSTEM', 'ORGANIZATION', 'SERVICE');

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.organization_id', true), '')::uuid;
END;
$$ LANGUAGE plpgsql STABLE;

-- -----------------------------------------------------------------------------
-- Reference tables (global schema catalog)
-- -----------------------------------------------------------------------------
CREATE TABLE configuration_schema (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key             TEXT NOT NULL,
    value_type      config_value_type NOT NULL DEFAULT 'STRING',
    default_value   JSONB,
    constraints     JSONB NOT NULL DEFAULT '{}'::jsonb,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_configuration_schema_key UNIQUE (key)
);

COMMENT ON TABLE configuration_schema IS 'Global catalog declaring every known configuration key, its type and validation constraints.';

-- -----------------------------------------------------------------------------
-- Master tables
-- -----------------------------------------------------------------------------
CREATE TABLE configurations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    scope           config_scope NOT NULL DEFAULT 'ORGANIZATION',
    key             TEXT NOT NULL,
    value           JSONB NOT NULL,
    description     TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE configurations IS 'Tenant scoped configuration values. SYSTEM scope rows carry a sentinel tenant id (all-zeros UUID) and are owned by the platform.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE configuration_history (
    id              BIGSERIAL PRIMARY KEY,
    configuration_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    key             TEXT NOT NULL,
    old_value       JSONB,
    new_value       JSONB,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL
);

COMMENT ON TABLE configuration_history IS 'Full version history of every configuration change.';

CREATE OR REPLACE FUNCTION audit_configuration_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO configuration_history(configuration_id, organization_id, key, old_value, new_value,
                                          changed_by, version)
        VALUES (OLD.id, OLD.organization_id, OLD.key, OLD.value, NEW.value,
                COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid), NEW.version);
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO configuration_history(configuration_id, organization_id, key, old_value, new_value,
                                          changed_by, version)
        VALUES (NEW.id, NEW.organization_id, NEW.key, NULL, NEW.value,
                COALESCE(NEW.created_by, current_setting('app.user_id', true)::uuid), NEW.version);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_configuration_history
    AFTER INSERT OR UPDATE ON configurations
    FOR EACH ROW EXECUTE FUNCTION audit_configuration_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_configurations_org_key ON configurations (organization_id, scope, key) WHERE deleted_at IS NULL;
CREATE INDEX idx_configurations_scope ON configurations (scope, key) WHERE deleted_at IS NULL;

CREATE INDEX idx_configuration_history_config ON configuration_history (configuration_id, version DESC);
CREATE INDEX idx_configuration_history_key ON configuration_history (key, changed_at DESC);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE configurations ENABLE ROW LEVEL SECURITY;

-- SYSTEM scope is visible to every tenant (read-only to non-platform users).
CREATE POLICY tenant_isolation_configurations ON configurations
    USING (scope = 'SYSTEM' OR organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
