-- =============================================================================
-- interview_db - Interviews, sessions, panels and feedback
-- Owning service: interview-service
-- Designed for millions of interviews: tenant-first composite indexes,
-- partial indexes on soft-deleted rows, JSONB metadata.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE interview_status AS ENUM ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW');

CREATE TYPE interview_mode AS ENUM ('ONLINE', 'ONSITE', 'HYBRID');

CREATE TYPE session_status AS ENUM ('PENDING', 'ACTIVE', 'PAUSED', 'ENDED', 'ABNORMAL');

CREATE TYPE feedback_status AS ENUM ('DRAFT', 'SUBMITTED');

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
CREATE TABLE interviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    candidate_id    UUID NOT NULL,
    recruiter_id    UUID NOT NULL,
    round_number    INT NOT NULL DEFAULT 1,
    title           TEXT NOT NULL,
    status          interview_status NOT NULL DEFAULT 'SCHEDULED',
    mode            interview_mode NOT NULL DEFAULT 'ONLINE',
    meeting_url     TEXT,
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    timezone        TEXT NOT NULL DEFAULT 'UTC',
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_interview_round CHECK (round_number > 0),
    CONSTRAINT chk_interview_time_window CHECK (ends_at > starts_at),
    CONSTRAINT chk_interview_title_not_blank CHECK (length(btrim(title)) > 0)
);

COMMENT ON TABLE interviews IS 'Interview master record. candidate_id and recruiter_id are soft references into candidate_db and recruiter_db.';

CREATE TABLE interview_sessions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL,
    interview_id      UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    session_token_hash TEXT NOT NULL,
    device_id         TEXT,
    client_version    TEXT,
    started_at        TIMESTAMPTZ,
    ended_at          TIMESTAMPTZ,
    status            session_status NOT NULL DEFAULT 'PENDING',
    heartbeat_cadence_seconds INT NOT NULL DEFAULT 5,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_heartbeat_cadence CHECK (heartbeat_cadence_seconds BETWEEN 1 AND 3600)
);

COMMENT ON TABLE interview_sessions IS 'One active monitoring session per interview run. session_token_hash stores a SHA-256 hash of the session token.';

CREATE TABLE interviewers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    full_name       TEXT NOT NULL,
    email           TEXT NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_interviewers_email_format CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$')
);

COMMENT ON TABLE interviewers IS 'People who conduct interviews. user_id is a soft reference into identity_db.users.';

-- -----------------------------------------------------------------------------
-- Bridge tables
-- -----------------------------------------------------------------------------
CREATE TABLE interview_panels (
    interview_id    UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    interviewer_id  UUID NOT NULL REFERENCES interviewers(id) ON DELETE CASCADE,
    role            TEXT NOT NULL DEFAULT 'PANELIST',
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (interview_id, interviewer_id)
);

COMMENT ON TABLE interview_panels IS 'Many-to-many bridge linking interviews to their interviewers.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE interview_feedback (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    interview_id    UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    interviewer_id  UUID NOT NULL REFERENCES interviewers(id),
    rating          INT,
    strengths       TEXT,
    concerns        TEXT,
    recommendation  TEXT,
    status          feedback_status NOT NULL DEFAULT 'DRAFT',
    submitted_at    TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
);

COMMENT ON TABLE interview_feedback IS 'Structured feedback collected after an interview.';

CREATE TABLE interview_calendar_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    interview_id    UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    provider        TEXT NOT NULL,
    provider_event_id TEXT NOT NULL,
    event_url       TEXT,
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    status          TEXT NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_calendar_provider_event UNIQUE (provider, provider_event_id)
);

COMMENT ON TABLE interview_calendar_events IS 'External calendar provider (Google, Outlook) event mirror.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE interviews_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    candidate_id    UUID,
    recruiter_id    UUID,
    round_number    INT,
    title           TEXT,
    status          interview_status,
    mode            interview_mode,
    meeting_url     TEXT,
    starts_at       TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_interviews_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO interviews_history(history_action, changed_by, id, organization_id, candidate_id,
                                       recruiter_id, round_number, title, status, mode, meeting_url,
                                       starts_at, ends_at, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.organization_id, OLD.candidate_id, OLD.recruiter_id, OLD.round_number,
                OLD.title, OLD.status, OLD.mode, OLD.meeting_url, OLD.starts_at, OLD.ends_at, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO interviews_history(history_action, changed_by, id, organization_id, candidate_id,
                                       recruiter_id, round_number, title, status, mode, meeting_url,
                                       starts_at, ends_at, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.organization_id, NEW.candidate_id, NEW.recruiter_id, NEW.round_number,
                NEW.title, NEW.status, NEW.mode, NEW.meeting_url, NEW.starts_at, NEW.ends_at, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_interviews_history
    AFTER INSERT OR UPDATE OR DELETE ON interviews
    FOR EACH ROW EXECUTE FUNCTION audit_interviews_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE INDEX idx_interviews_org_status ON interviews (organization_id, status, starts_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_org_candidate ON interviews (organization_id, candidate_id, starts_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_org_recruiter ON interviews (organization_id, recruiter_id, starts_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_org_time ON interviews (organization_id, starts_at, ends_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_candidate_lookup ON interviews (candidate_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_metadata ON interviews USING GIN (metadata jsonb_path_ops);

CREATE INDEX idx_interview_sessions_interview ON interview_sessions (interview_id, status);
CREATE INDEX idx_interview_sessions_org ON interview_sessions (organization_id, started_at DESC);
CREATE INDEX idx_interview_sessions_token ON interview_sessions (session_token_hash);
CREATE INDEX idx_interview_sessions_active ON interview_sessions (status) WHERE status IN ('ACTIVE', 'PAUSED');

CREATE INDEX idx_interviewers_org ON interviewers (organization_id, lower(email)) WHERE deleted_at IS NULL;

CREATE INDEX idx_interview_panels_interviewer ON interview_panels (interviewer_id);

CREATE INDEX idx_interview_feedback_interview ON interview_feedback (interview_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_interview_feedback_interviewer ON interview_feedback (interviewer_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_interview_calendar_interview ON interview_calendar_events (interview_id, starts_at);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE interviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE interviewers ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_panels ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_calendar_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_interviews ON interviews
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_interview_sessions ON interview_sessions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_interviewers ON interviewers
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_interview_panels ON interview_panels
    USING (EXISTS (SELECT 1 FROM interviews i WHERE i.id = interview_id AND i.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM interviews i WHERE i.id = interview_id AND i.organization_id = current_tenant_id()));

CREATE POLICY tenant_isolation_interview_feedback ON interview_feedback
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_interview_calendar_events ON interview_calendar_events
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
