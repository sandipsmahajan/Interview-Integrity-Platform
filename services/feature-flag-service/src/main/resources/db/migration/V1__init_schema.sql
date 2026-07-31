-- =============================================================================
-- feature_flag_db - Feature flags, variants, targets and experiments
-- Owning service: feature-flag-service
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE flag_kind AS ENUM ('BOOLEAN', 'STRING', 'NUMBER', 'JSON');

CREATE TYPE experiment_status AS ENUM ('DRAFT', 'RUNNING', 'PAUSED', 'COMPLETED', 'REJECTED');

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
-- Master tables
-- -----------------------------------------------------------------------------
CREATE TABLE features (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    kind            flag_kind NOT NULL DEFAULT 'BOOLEAN',
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_features_code_format CHECK (code ~ '^[a-z][a-z0-9._-]{1,63}$')
);

COMMENT ON TABLE features IS 'Feature catalog per tenant.';

CREATE TABLE feature_flags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    feature_id      UUID NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    environment     TEXT NOT NULL DEFAULT 'PRODUCTION',
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percent INT NOT NULL DEFAULT 0,
    default_variant TEXT,
    variants        JSONB NOT NULL DEFAULT '{}'::jsonb,
    rules           JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_rollout_percent CHECK (rollout_percent BETWEEN 0 AND 100),
    CONSTRAINT uq_feature_flags UNIQUE (feature_id, environment)
);

COMMENT ON TABLE feature_flags IS 'Flag configuration per feature and environment. rules carries targeting rules; rollout_percent enables gradual rollouts.';

-- -----------------------------------------------------------------------------
-- Bridge tables
-- -----------------------------------------------------------------------------
CREATE TABLE flag_targets (
    flag_id     UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    variant     TEXT,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    added_by    UUID,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (flag_id, user_id)
);

COMMENT ON TABLE flag_targets IS 'Explicit per-user overrides for a flag. user_id is a soft reference into identity_db.users.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE experiments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            TEXT NOT NULL,
    feature_id      UUID NOT NULL REFERENCES features(id) ON DELETE CASCADE,
    control_variant TEXT,
    treatment_variant TEXT,
    status          experiment_status NOT NULL DEFAULT 'DRAFT',
    started_at      TIMESTAMPTZ,
    ended_at        TIMESTAMPTZ,
    metrics         JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE experiments IS 'A/B experiments driving evidence based flag rollouts.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE feature_flags_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    feature_id      UUID,
    environment     TEXT,
    enabled         BOOLEAN,
    rollout_percent INT,
    default_variant TEXT,
    variants        JSONB,
    rules           JSONB,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_feature_flags_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO feature_flags_history(history_action, changed_by, id, organization_id, feature_id,
                                      environment, enabled, rollout_percent, default_variant,
                                      variants, rules, version)
    VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
            NEW.id, NEW.organization_id, NEW.feature_id, NEW.environment, NEW.enabled,
            NEW.rollout_percent, NEW.default_variant, NEW.variants, NEW.rules, NEW.version);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_feature_flags_history
    AFTER INSERT OR UPDATE ON feature_flags
    FOR EACH ROW EXECUTE FUNCTION audit_feature_flags_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_features_org_code ON features (organization_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_features_org ON features (organization_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_feature_flags_org_env ON feature_flags (organization_id, environment, enabled);
CREATE INDEX idx_feature_flags_rules ON feature_flags USING GIN (rules jsonb_path_ops);
CREATE INDEX idx_feature_flags_variants ON feature_flags USING GIN (variants jsonb_path_ops);

CREATE INDEX idx_flag_targets_user ON flag_targets (user_id);

CREATE INDEX idx_experiments_org ON experiments (organization_id, status);
CREATE INDEX idx_experiments_feature ON experiments (feature_id);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE features ENABLE ROW LEVEL SECURITY;
ALTER TABLE feature_flags ENABLE ROW LEVEL SECURITY;
ALTER TABLE flag_targets ENABLE ROW LEVEL SECURITY;
ALTER TABLE experiments ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_features ON features
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_feature_flags ON feature_flags
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_flag_targets ON flag_targets
    USING (EXISTS (SELECT 1 FROM feature_flags f WHERE f.id = flag_id AND f.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM feature_flags f WHERE f.id = flag_id AND f.organization_id = current_tenant_id()));

CREATE POLICY tenant_isolation_experiments ON experiments
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
