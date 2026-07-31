-- =============================================================================
-- report_db - Report requests, definitions, schedules and generated artifacts
-- Owning service: report-service
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE report_status AS ENUM ('REQUESTED', 'GENERATING', 'READY', 'FAILED', 'EXPIRED');

CREATE TYPE report_format AS ENUM ('PDF', 'XLSX', 'CSV', 'JSON');

CREATE TYPE report_type AS ENUM ('SESSION', 'CANDIDATE', 'INTERVIEW', 'RECRUITER', 'ORGANIZATION', 'INTEGRITY');

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
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    type            report_type NOT NULL,
    title           TEXT NOT NULL,
    status          report_status NOT NULL DEFAULT 'REQUESTED',
    format          report_format NOT NULL DEFAULT 'PDF',
    score           NUMERIC(5,2),
    filters         JSONB NOT NULL DEFAULT '{}'::jsonb,
    requested_by    UUID,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    generated_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    storage_object_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_report_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
);

COMMENT ON TABLE reports IS 'Generated report artifacts. storage_object_id soft-references storage_db.storage_objects.';

CREATE TABLE report_sections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    report_id       UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    section_type    TEXT NOT NULL,
    title           TEXT,
    content         JSONB NOT NULL,
    order_index     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_report_sections_order CHECK (order_index >= 0)
);

COMMENT ON TABLE report_sections IS 'Ordered sections of a generated report. content carries the section payload.';

CREATE TABLE report_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    report_id       UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    aggregation_level TEXT NOT NULL DEFAULT 'SESSION',
    time_range      JSONB NOT NULL DEFAULT '{}'::jsonb,
    parameters      JSONB NOT NULL DEFAULT '{}'::jsonb,
    requested_by    UUID,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE report_requests IS 'Parameters used to generate a report, kept for reproducibility.';

CREATE TABLE report_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    type            report_type NOT NULL,
    cron_expression TEXT NOT NULL,
    format          report_format NOT NULL DEFAULT 'PDF',
    recipients      JSONB NOT NULL DEFAULT '[]'::jsonb,
    parameters      JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    next_run_at     TIMESTAMPTZ,
    last_run_at     TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE report_schedules IS 'Recurring report definitions evaluated by scheduler-service.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE reports_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    type            report_type,
    title           TEXT,
    status          report_status,
    format          report_format,
    score           NUMERIC(5,2),
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_reports_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO reports_history(history_action, changed_by, id, organization_id, type, title,
                                status, format, score, version)
    VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
            NEW.id, NEW.organization_id, NEW.type, NEW.title, NEW.status, NEW.format,
            NEW.score, NEW.version);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reports_history
    AFTER INSERT OR UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION audit_reports_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE INDEX idx_reports_org_status ON reports (organization_id, status, requested_at DESC);
CREATE INDEX idx_reports_org_type ON reports (organization_id, type, requested_at DESC);
CREATE INDEX idx_reports_requested_by ON reports (requested_by, requested_at DESC);
CREATE INDEX idx_reports_expiry ON reports (expires_at) WHERE status = 'READY';

CREATE INDEX idx_report_sections_report ON report_sections (report_id, order_index);

CREATE INDEX idx_report_requests_report ON report_requests (report_id);

CREATE INDEX idx_report_schedules_org ON report_schedules (organization_id, enabled) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_schedules_next_run ON report_schedules (next_run_at) WHERE enabled AND deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_sections ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_schedules ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_reports ON reports
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_report_sections ON report_sections
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_report_requests ON report_requests
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_report_schedules ON report_schedules
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
