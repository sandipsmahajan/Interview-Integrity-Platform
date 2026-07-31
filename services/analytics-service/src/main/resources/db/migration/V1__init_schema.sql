-- =============================================================================
-- analytics_db - Pre-aggregated summaries for dashboards and reporting
-- Owning service: analytics-service
--
-- Raw events live in telemetry_db / policy_db. The analytics service consumes
-- Kafka events and upserts daily summaries here (event-driven aggregation).
-- Weekly and monthly rollups are derived from the daily tables through views
-- and materialized views. This keeps query time constant regardless of the
-- volume of raw events.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
-- Daily summary tables
-- -----------------------------------------------------------------------------
CREATE TABLE daily_organization_summaries (
    summary_date        DATE NOT NULL,
    organization_id     UUID NOT NULL,
    interviews_scheduled BIGINT NOT NULL DEFAULT 0,
    interviews_completed BIGINT NOT NULL DEFAULT 0,
    interviews_cancelled BIGINT NOT NULL DEFAULT 0,
    candidates_active   BIGINT NOT NULL DEFAULT 0,
    recruiters_active   BIGINT NOT NULL DEFAULT 0,
    violations          BIGINT NOT NULL DEFAULT 0,
    avg_integrity_score NUMERIC(5,2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (summary_date, organization_id)
);

COMMENT ON TABLE daily_organization_summaries IS 'Per-organization daily operational summary.';

CREATE TABLE daily_recruiter_summaries (
    summary_date        DATE NOT NULL,
    organization_id     UUID NOT NULL,
    recruiter_id        UUID NOT NULL,
    interviews_held     BIGINT NOT NULL DEFAULT 0,
    interviews_completed BIGINT NOT NULL DEFAULT 0,
    candidates_contacted BIGINT NOT NULL DEFAULT 0,
    avg_feedback_rating NUMERIC(3,2),
    violations          BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (summary_date, organization_id, recruiter_id)
);

COMMENT ON TABLE daily_recruiter_summaries IS 'Per-recruiter daily performance summary.';

CREATE TABLE daily_candidate_summaries (
    summary_date        DATE NOT NULL,
    organization_id     UUID NOT NULL,
    candidate_id        UUID NOT NULL,
    interviews_attended BIGINT NOT NULL DEFAULT 0,
    avg_score           NUMERIC(5,2),
    assessments_completed BIGINT NOT NULL DEFAULT 0,
    violations          BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (summary_date, organization_id, candidate_id)
);

COMMENT ON TABLE daily_candidate_summaries IS 'Per-candidate daily summary.';

CREATE TABLE daily_interview_summaries (
    summary_date        DATE NOT NULL,
    organization_id     UUID NOT NULL,
    interview_id        UUID NOT NULL,
    duration_minutes    INT,
    integrity_score     NUMERIC(5,2),
    violations          BIGINT NOT NULL DEFAULT 0,
    status              TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (summary_date, organization_id, interview_id)
);

COMMENT ON TABLE daily_interview_summaries IS 'Per-interview daily summary (one row per interview on its completion day).';

CREATE TABLE daily_integrity_summaries (
    summary_date       DATE NOT NULL,
    organization_id    UUID NOT NULL,
    total_events       BIGINT NOT NULL DEFAULT 0,
    violations_total   BIGINT NOT NULL DEFAULT 0,
    violations_by_severity JSONB NOT NULL DEFAULT '{}'::jsonb,
    violations_by_rule JSONB NOT NULL DEFAULT '{}'::jsonb,
    sessions_started   BIGINT NOT NULL DEFAULT 0,
    sessions_abandoned BIGINT NOT NULL DEFAULT 0,
    avg_heartbeat_cadence_seconds NUMERIC(8,2),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (summary_date, organization_id)
);

COMMENT ON TABLE daily_integrity_summaries IS 'Per-organization daily integrity scorecard. violations_by_severity/rule are JSONB counters.';

-- -----------------------------------------------------------------------------
-- Weekly / monthly rollups (views over the daily tables)
-- -----------------------------------------------------------------------------
CREATE VIEW weekly_organization_summaries AS
SELECT
    date_trunc('week', summary_date)::date AS summary_week,
    organization_id,
    sum(interviews_scheduled)  AS interviews_scheduled,
    sum(interviews_completed)  AS interviews_completed,
    sum(interviews_cancelled)  AS interviews_cancelled,
    sum(violations)            AS violations,
    avg(avg_integrity_score)   AS avg_integrity_score
FROM daily_organization_summaries
GROUP BY date_trunc('week', summary_date), organization_id;

CREATE VIEW weekly_recruiter_summaries AS
SELECT
    date_trunc('week', summary_date)::date AS summary_week,
    organization_id,
    recruiter_id,
    sum(interviews_held)      AS interviews_held,
    sum(interviews_completed) AS interviews_completed,
    sum(candidates_contacted) AS candidates_contacted,
    avg(avg_feedback_rating)  AS avg_feedback_rating,
    sum(violations)           AS violations
FROM daily_recruiter_summaries
GROUP BY date_trunc('week', summary_date), organization_id, recruiter_id;

CREATE VIEW weekly_integrity_summaries AS
SELECT
    date_trunc('week', summary_date)::date AS summary_week,
    organization_id,
    sum(total_events)     AS total_events,
    sum(violations_total) AS violations_total,
    sum(sessions_started) AS sessions_started,
    sum(sessions_abandoned) AS sessions_abandoned
FROM daily_integrity_summaries
GROUP BY date_trunc('week', summary_date), organization_id;

-- -----------------------------------------------------------------------------
-- Materialized views (monthly rollups, refreshed by scheduler-service)
-- -----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW mv_monthly_organization_summaries AS
SELECT
    date_trunc('month', summary_date)::date AS summary_month,
    organization_id,
    sum(interviews_scheduled) AS interviews_scheduled,
    sum(interviews_completed) AS interviews_completed,
    sum(interviews_cancelled) AS interviews_cancelled,
    sum(violations)           AS violations,
    avg(avg_integrity_score)  AS avg_integrity_score,
    count(*)                  AS days_reported
FROM daily_organization_summaries
GROUP BY date_trunc('month', summary_date), organization_id
WITH NO DATA;

CREATE MATERIALIZED VIEW mv_monthly_integrity_summaries AS
SELECT
    date_trunc('month', summary_date)::date AS summary_month,
    organization_id,
    sum(total_events)     AS total_events,
    sum(violations_total) AS violations_total,
    sum(sessions_started) AS sessions_started,
    sum(sessions_abandoned) AS sessions_abandoned,
    avg(avg_heartbeat_cadence_seconds) AS avg_heartbeat_cadence_seconds
FROM daily_integrity_summaries
GROUP BY date_trunc('month', summary_date), organization_id
WITH NO DATA;

CREATE INDEX idx_mv_monthly_org ON mv_monthly_organization_summaries (organization_id, summary_month DESC);
CREATE INDEX idx_mv_monthly_integrity ON mv_monthly_integrity_summaries (organization_id, summary_month DESC);

-- -----------------------------------------------------------------------------
-- Aggregation helpers
-- -----------------------------------------------------------------------------
-- Upserts a single daily organization summary (idempotent per primary key).
CREATE OR REPLACE FUNCTION analytics_upsert_organization_daily(
    p_summary_date DATE, p_organization_id UUID,
    p_interviews_scheduled BIGINT, p_interviews_completed BIGINT,
    p_interviews_cancelled BIGINT, p_candidates_active BIGINT,
    p_recruiters_active BIGINT, p_violations BIGINT, p_avg_score NUMERIC)
RETURNS VOID AS $$
BEGIN
    INSERT INTO daily_organization_summaries
        (summary_date, organization_id, interviews_scheduled, interviews_completed,
         interviews_cancelled, candidates_active, recruiters_active, violations, avg_integrity_score)
    VALUES
        (p_summary_date, p_organization_id, p_interviews_scheduled, p_interviews_completed,
         p_interviews_cancelled, p_candidates_active, p_recruiters_active, p_violations, p_avg_score)
    ON CONFLICT (summary_date, organization_id) DO UPDATE SET
        interviews_scheduled  = EXCLUDED.interviews_scheduled,
        interviews_completed  = EXCLUDED.interviews_completed,
        interviews_cancelled  = EXCLUDED.interviews_cancelled,
        candidates_active     = EXCLUDED.candidates_active,
        recruiters_active     = EXCLUDED.recruiters_active,
        violations            = EXCLUDED.violations,
        avg_integrity_score   = EXCLUDED.avg_integrity_score,
        updated_at            = now();
END;
$$ LANGUAGE plpgsql;

-- Refreshes the monthly materialized views. Called by scheduler-service.
CREATE OR REPLACE FUNCTION analytics_refresh_monthly_views()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW mv_monthly_organization_summaries;
    REFRESH MATERIALIZED VIEW mv_monthly_integrity_summaries;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- Job tracking
-- -----------------------------------------------------------------------------
CREATE TABLE analytics_job_runs (
    id               BIGSERIAL PRIMARY KEY,
    job_name         TEXT NOT NULL,
    status           TEXT NOT NULL,
    records_processed BIGINT NOT NULL DEFAULT 0,
    error_message    TEXT,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at      TIMESTAMPTZ,
    duration_ms      BIGINT
);

COMMENT ON TABLE analytics_job_runs IS 'Observability log of analytics aggregation runs.';

CREATE INDEX idx_analytics_job_runs_name ON analytics_job_runs (job_name, started_at DESC);

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE INDEX idx_daily_recruiter_summaries_org ON daily_recruiter_summaries (organization_id, summary_date DESC);
CREATE INDEX idx_daily_recruiter_summaries_recruiter ON daily_recruiter_summaries (recruiter_id, summary_date DESC);
CREATE INDEX idx_daily_candidate_summaries_candidate ON daily_candidate_summaries (candidate_id, summary_date DESC);
CREATE INDEX idx_daily_interview_summaries_interview ON daily_interview_summaries (interview_id);
CREATE INDEX idx_daily_interview_summaries_org_date ON daily_interview_summaries (organization_id, summary_date DESC);
CREATE INDEX idx_daily_integrity_summaries_org ON daily_integrity_summaries (organization_id, summary_date DESC);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE daily_organization_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_recruiter_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_candidate_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_interview_summaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_integrity_summaries ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_daily_organization ON daily_organization_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_daily_recruiter ON daily_recruiter_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_daily_candidate ON daily_candidate_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_daily_interview ON daily_interview_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_daily_integrity ON daily_integrity_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
