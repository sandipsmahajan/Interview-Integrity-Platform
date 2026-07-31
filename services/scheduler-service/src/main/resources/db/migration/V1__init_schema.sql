-- =============================================================================
-- scheduler_db - Scheduled jobs, executions and distributed locks
-- Owning service: scheduler-service
-- Supports cron driven and one-off jobs with distributed locking so only one
-- worker executes a job at a time even with multiple replicas.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE job_status AS ENUM ('ENABLED', 'DISABLED', 'PAUSED');

CREATE TYPE execution_status AS ENUM ('RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT', 'SKIPPED');

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
CREATE TABLE scheduled_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name            TEXT NOT NULL,
    job_type        TEXT NOT NULL,
    cron_expression TEXT,
    handler         TEXT NOT NULL,
    payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    status          job_status NOT NULL DEFAULT 'ENABLED',
    max_retries     INT NOT NULL DEFAULT 0,
    timeout_seconds INT NOT NULL DEFAULT 300,
    retry_count     INT NOT NULL DEFAULT 0,
    last_run_at     TIMESTAMPTZ,
    last_run_status execution_status,
    next_run_at     TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_job_max_retries CHECK (max_retries >= 0),
    CONSTRAINT chk_job_timeout CHECK (timeout_seconds > 0)
);

COMMENT ON TABLE scheduled_jobs IS 'Job definitions. handler names the executable job handler, payload carries handler arguments.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE job_executions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    job_id          UUID NOT NULL REFERENCES scheduled_jobs(id) ON DELETE CASCADE,
    status          execution_status NOT NULL DEFAULT 'RUNNING',
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    exit_code       INT,
    error_message   TEXT,
    duration_ms     BIGINT,
    worker_id       TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE job_executions IS 'Execution history per job (1 job : N executions). Retained for observability and audit.';

CREATE TABLE job_locks (
    job_id      UUID PRIMARY KEY REFERENCES scheduled_jobs(id) ON DELETE CASCADE,
    lock_token  TEXT NOT NULL,
    owner_id    TEXT NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE job_locks IS 'Distributed lock table. Acquiring = INSERT ... ON CONFLICT DO NOTHING with an expiry window; the owner renews and releases.';

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE INDEX idx_scheduled_jobs_org ON scheduled_jobs (organization_id, job_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_scheduled_jobs_next_run ON scheduled_jobs (next_run_at) WHERE status = 'ENABLED' AND deleted_at IS NULL;
CREATE INDEX idx_scheduled_jobs_handler ON scheduled_jobs (handler) WHERE status = 'ENABLED' AND deleted_at IS NULL;

CREATE INDEX idx_job_executions_job ON job_executions (job_id, started_at DESC);
CREATE INDEX idx_job_executions_org_time ON job_executions (organization_id, started_at DESC);
CREATE INDEX idx_job_executions_status ON job_executions (status, started_at) WHERE status = 'RUNNING';

CREATE INDEX idx_job_locks_expiry ON job_locks (expires_at);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE scheduled_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_executions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_scheduled_jobs ON scheduled_jobs
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_job_executions ON job_executions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
