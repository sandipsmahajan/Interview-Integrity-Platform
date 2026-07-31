-- =============================================================================
-- candidate_db - Candidate profiles, documents, assessments and tags
-- Owning service: candidate-service
-- Designed for millions of candidate rows: composite tenant indexes, GIN on
-- JSONB/skill arrays, partial indexes on soft-deleted rows.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE candidate_status AS ENUM ('NEW', 'SCREENING', 'INTERVIEWING', 'OFFERED', 'HIRED', 'REJECTED', 'ARCHIVED');

CREATE TYPE assessment_status AS ENUM ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED');

CREATE TYPE consent_status AS ENUM ('GRANTED', 'REVOKED');

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
CREATE TABLE candidates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID,
    email           TEXT NOT NULL,
    full_name       TEXT NOT NULL,
    phone           TEXT,
    status          candidate_status NOT NULL DEFAULT 'NEW',
    source          TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_candidates_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
    CONSTRAINT chk_candidates_full_name_not_blank CHECK (length(btrim(full_name)) > 0)
);

COMMENT ON TABLE candidates IS 'Candidate master record. user_id is a soft reference into identity_db.users.';

CREATE TABLE candidate_profiles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id     UUID NOT NULL UNIQUE REFERENCES candidates(id) ON DELETE CASCADE,
    organization_id  UUID NOT NULL,
    headline         TEXT,
    bio              TEXT,
    location         TEXT,
    timezone         TEXT,
    resume_summary   TEXT,
    linkedin_url     TEXT,
    github_url       TEXT,
    skills           TEXT[] NOT NULL DEFAULT '{}',
    experience_years NUMERIC(4,1),
    attributes       JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_experience_years CHECK (experience_years IS NULL OR experience_years BETWEEN 0 AND 60)
);

COMMENT ON TABLE candidate_profiles IS 'One-to-one extended candidate profile. skills array is GIN indexed.';

CREATE TABLE candidate_documents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL,
    candidate_id     UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    storage_object_id UUID NOT NULL,
    name             TEXT NOT NULL,
    content_type     TEXT,
    size_bytes       BIGINT NOT NULL,
    uploaded_by      UUID,
    uploaded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by       UUID,
    deleted_at       TIMESTAMPTZ,
    version          BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_document_size CHECK (size_bytes >= 0)
);

COMMENT ON TABLE candidate_documents IS 'Candidate attachments. storage_object_id soft-references storage_db.storage_objects.';

CREATE TABLE candidate_notes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    candidate_id    UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL,
    body            TEXT NOT NULL,
    pinned          BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE candidate_notes IS 'Collaboration notes on a candidate. author_id is a soft reference into identity_db.users.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE assessments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    candidate_id    UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    assessment_type TEXT NOT NULL,
    status          assessment_status NOT NULL DEFAULT 'ASSIGNED',
    score           NUMERIC(5,2),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    assigned_by     UUID,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_assessment_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
);

COMMENT ON TABLE assessments IS 'Skill/behaviour assessments assigned to a candidate.';

CREATE TABLE candidate_consents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    candidate_id    UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    consent_type    TEXT NOT NULL,
    status          consent_status NOT NULL DEFAULT 'GRANTED',
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by      UUID,
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID,
    consent_version TEXT NOT NULL DEFAULT '1.0',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_candidate_consents UNIQUE (candidate_id, consent_type)
);

COMMENT ON TABLE candidate_consents IS 'Data-protection consents (monitoring, data sharing) with versioning.';

-- -----------------------------------------------------------------------------
-- Tags (bridge)
-- -----------------------------------------------------------------------------
CREATE TABLE tags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_tags_org_code UNIQUE (organization_id, code)
);

CREATE TABLE candidate_tags (
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    tag_id       UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    tagged_by    UUID,
    tagged_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (candidate_id, tag_id)
);

COMMENT ON TABLE candidate_tags IS 'Many-to-many bridge between candidates and tags.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE candidates_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    user_id         UUID,
    email           TEXT,
    full_name       TEXT,
    phone           TEXT,
    status          candidate_status,
    source          TEXT,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_candidates_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO candidates_history(history_action, changed_by, id, organization_id, user_id,
                                       email, full_name, phone, status, source, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.organization_id, OLD.user_id, OLD.email, OLD.full_name, OLD.phone,
                OLD.status, OLD.source, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO candidates_history(history_action, changed_by, id, organization_id, user_id,
                                       email, full_name, phone, status, source, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.organization_id, NEW.user_id, NEW.email, NEW.full_name, NEW.phone,
                NEW.status, NEW.source, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_candidates_history
    AFTER INSERT OR UPDATE OR DELETE ON candidates
    FOR EACH ROW EXECUTE FUNCTION audit_candidates_history();

-- -----------------------------------------------------------------------------
-- Index strategy (millions of rows)
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_candidates_org_email ON candidates (organization_id, lower(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_org_status ON candidates (organization_id, status, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_org_name ON candidates (organization_id, full_name) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_user ON candidates (user_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_candidate_profiles_skills ON candidate_profiles USING GIN (skills);
CREATE INDEX idx_candidate_profiles_org ON candidate_profiles (organization_id);
CREATE INDEX idx_candidate_profiles_attributes ON candidate_profiles USING GIN (attributes jsonb_path_ops);

CREATE INDEX idx_candidate_documents_candidate ON candidate_documents (candidate_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidate_documents_org ON candidate_documents (organization_id, uploaded_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX idx_candidate_notes_candidate ON candidate_notes (candidate_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidate_notes_author ON candidate_notes (author_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_assessments_candidate ON assessments (candidate_id, status);
CREATE INDEX idx_assessments_org_type ON assessments (organization_id, assessment_type, status);

CREATE INDEX idx_candidate_consents_candidate ON candidate_consents (candidate_id, status);

CREATE INDEX idx_candidate_tags_tag ON candidate_tags (tag_id);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_consents ENABLE ROW LEVEL SECURITY;
ALTER TABLE tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidate_tags ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_candidates ON candidates
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_profiles ON candidate_profiles
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_documents ON candidate_documents
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_notes ON candidate_notes
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_assessments ON assessments
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_consents ON candidate_consents
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_tags ON tags
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_candidate_tags ON candidate_tags
    USING (EXISTS (SELECT 1 FROM candidates c WHERE c.id = candidate_id AND c.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM candidates c WHERE c.id = candidate_id AND c.organization_id = current_tenant_id()));
