-- =============================================================================
-- recruiter_db - Recruiter profiles, pipeline stages and assignments
-- Owning service: recruiter-service
-- Cross-database references (user_id -> identity_db.users, candidate_id ->
-- candidate_db.candidates) are SOFT references: UUID columns without FKs.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE recruiter_status AS ENUM ('ACTIVE', 'ON_LEAVE', 'INACTIVE');

CREATE TYPE pipeline_status AS ENUM ('CURRENT', 'PAST');

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
CREATE TABLE recruiters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    full_name       TEXT NOT NULL,
    email           TEXT NOT NULL,
    title           TEXT,
    status          recruiter_status NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_recruiters_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_recruiters_full_name_not_blank CHECK (length(btrim(full_name)) > 0)
);

COMMENT ON TABLE recruiters IS 'Recruiter profile. user_id soft-references identity_db.users.';

CREATE TABLE recruiter_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recruiter_id    UUID NOT NULL UNIQUE REFERENCES recruiters(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    bio             TEXT,
    specialties     TEXT[] NOT NULL DEFAULT '{}',
    linkedin_url    TEXT,
    availability    JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_linkedin_url CHECK (linkedin_url IS NULL OR linkedin_url ~ '^https?://')
);

COMMENT ON TABLE recruiter_profiles IS 'One-to-one extended recruiter profile. specialties is a text array indexed with GIN.';

CREATE TABLE pipeline_stages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    order_index     INT NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_pipeline_stages_org_code UNIQUE (organization_id, code)
);

COMMENT ON TABLE pipeline_stages IS 'Configurable hiring pipeline stages per tenant.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE candidate_pipeline (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    candidate_id    UUID NOT NULL,
    recruiter_id    UUID NOT NULL REFERENCES recruiters(id),
    stage_id        UUID NOT NULL REFERENCES pipeline_stages(id),
    position        INT NOT NULL DEFAULT 0,
    status          pipeline_status NOT NULL DEFAULT 'CURRENT',
    entered_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    exited_at       TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_candidate_pipeline_position CHECK (position >= 0)
);

COMMENT ON TABLE candidate_pipeline IS 'Tracks candidate movement through pipeline stages. candidate_id soft-references candidate_db.candidates.';

CREATE TABLE recruiter_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    recruiter_id    UUID NOT NULL REFERENCES recruiters(id) ON DELETE CASCADE,
    candidate_id    UUID NOT NULL,
    body            TEXT NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_recruiter_notes_body_not_blank CHECK (length(btrim(body)) > 0)
);

COMMENT ON TABLE recruiter_notes IS 'Private notes attached to a candidate by a recruiter.';

CREATE TABLE recruiter_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    recruiter_id    UUID NOT NULL REFERENCES recruiters(id) ON DELETE CASCADE,
    candidate_id    UUID NOT NULL,
    role            TEXT NOT NULL DEFAULT 'PRIMARY',
    assigned_by     UUID,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE recruiter_assignments IS 'Explicit assignment of a candidate to a recruiter with a role.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE recruiters_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    user_id         UUID,
    full_name       TEXT,
    email           TEXT,
    title           TEXT,
    status          recruiter_status,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_recruiters_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO recruiters_history(history_action, changed_by, id, organization_id, user_id,
                                       full_name, email, title, status, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.organization_id, OLD.user_id, OLD.full_name, OLD.email, OLD.title,
                OLD.status, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO recruiters_history(history_action, changed_by, id, organization_id, user_id,
                                       full_name, email, title, status, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.organization_id, NEW.user_id, NEW.full_name, NEW.email, NEW.title,
                NEW.status, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recruiters_history
    AFTER INSERT OR UPDATE OR DELETE ON recruiters
    FOR EACH ROW EXECUTE FUNCTION audit_recruiters_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_recruiters_org_user ON recruiters (organization_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_recruiters_org_email ON recruiters (organization_id, lower(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_recruiters_org_status ON recruiters (organization_id, status) WHERE deleted_at IS NULL;

CREATE INDEX idx_recruiter_profiles_specialties ON recruiter_profiles USING GIN (specialties);
CREATE INDEX idx_recruiter_profiles_org ON recruiter_profiles (organization_id);

CREATE INDEX idx_pipeline_stages_org ON pipeline_stages (organization_id, order_index) WHERE deleted_at IS NULL;

CREATE INDEX idx_candidate_pipeline_candidate ON candidate_pipeline (candidate_id, status);
CREATE INDEX idx_candidate_pipeline_stage ON candidate_pipeline (stage_id, status);
CREATE INDEX idx_candidate_pipeline_recruiter ON candidate_pipeline (recruiter_id, status);
CREATE INDEX idx_candidate_pipeline_org ON candidate_pipeline (organization_id, status);

CREATE INDEX idx_recruiter_notes_candidate ON recruiter_notes (candidate_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_recruiter_notes_recruiter ON recruiter_notes (recruiter_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX idx_recruiter_assignments_candidate ON recruiter_assignments (candidate_id) WHERE ended_at IS NULL;
CREATE INDEX idx_recruiter_assignments_recruiter ON recruiter_assignments (recruiter_id) WHERE ended_at IS NULL;

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE recruiters ENABLE ROW LEVEL SECURITY;
ALTER TABLE recruiter_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE pipeline_stages ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_pipeline ENABLE ROW LEVEL SECURITY;
ALTER TABLE recruiter_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recruiter_assignments ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_recruiters ON recruiters
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_recruiter_profiles ON recruiter_profiles
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_pipeline_stages ON pipeline_stages
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_pipeline ON candidate_pipeline
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_recruiter_notes ON recruiter_notes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_recruiter_assignments ON recruiter_assignments
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
