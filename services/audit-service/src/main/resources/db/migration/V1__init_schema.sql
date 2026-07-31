-- =============================================================================
-- audit_db - Central compliance audit trail and API access log
-- Owning service: audit-service
-- Every service publishes audit events to Kafka; audit-service persists them
-- here. audit_events and api_audit_log are partitioned by month and support
-- long retention with periodic archival.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE audit_outcome AS ENUM ('SUCCESS', 'FAILURE', 'DENIED');

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
-- Audit event log (append-heavy, partitioned by month)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_events (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    actor_id        UUID,
    actor_type      TEXT NOT NULL DEFAULT 'USER',
    action          TEXT NOT NULL,
    resource_type   TEXT NOT NULL,
    resource_id     UUID,
    outcome         audit_outcome NOT NULL DEFAULT 'SUCCESS',
    occurred_at     TIMESTAMPTZ NOT NULL,
    request_id      TEXT,
    ip_address      INET,
    user_agent      TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

COMMENT ON TABLE audit_events IS 'Compliance audit trail. Partitioned by month on occurred_at.';

CREATE OR REPLACE FUNCTION audit_partition_name(p_table TEXT, p_month DATE)
RETURNS TEXT AS $$
BEGIN
    RETURN p_table || '_' || to_char(p_month, 'YYYY_MM');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION audit_create_partitions(p_lookahead_months INT DEFAULT 3)
RETURNS VOID AS $$
DECLARE
    v_month DATE;
    v_start TEXT;
    v_end TEXT;
    v_name TEXT;
BEGIN
    v_month := date_trunc('month', current_date);
    FOR i IN 0..(p_lookahead_months - 1) LOOP
        v_name := audit_partition_name('audit_events', (v_month + (i * interval '1 month'))::date);
        v_start := to_char(date_trunc('month', v_month + (i * interval '1 month')), 'YYYY-MM-DD');
        v_end := to_char(date_trunc('month', v_month + ((i + 1) * interval '1 month')), 'YYYY-MM-DD');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF audit_events FOR VALUES FROM (%L) TO (%L)',
                v_name, v_start, v_end);
            EXECUTE format(
                'CREATE INDEX %I ON %I (organization_id, occurred_at DESC)',
                v_name || '_org_time', v_name);
            EXECUTE format(
                'CREATE INDEX %I ON %I (actor_id, occurred_at DESC)',
                v_name || '_actor_time', v_name);
            EXECUTE format(
                'CREATE INDEX %I ON %I (resource_type, resource_id, occurred_at DESC)',
                v_name || '_resource', v_name);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE audit_event_changes (
    id              BIGSERIAL,
    audit_event_id  UUID NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    field           TEXT NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

COMMENT ON TABLE audit_event_changes IS 'Field-level change detail for audit events. Partitioned alongside the parent.';

CREATE OR REPLACE FUNCTION audit_create_change_partitions(p_lookahead_months INT DEFAULT 3)
RETURNS VOID AS $$
DECLARE
    v_month DATE;
    v_start TEXT;
    v_end TEXT;
    v_name TEXT;
BEGIN
    v_month := date_trunc('month', current_date);
    FOR i IN 0..(p_lookahead_months - 1) LOOP
        v_name := audit_partition_name('audit_event_changes', (v_month + (i * interval '1 month'))::date);
        v_start := to_char(date_trunc('month', v_month + (i * interval '1 month')), 'YYYY-MM-DD');
        v_end := to_char(date_trunc('month', v_month + ((i + 1) * interval '1 month')), 'YYYY-MM-DD');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF audit_event_changes FOR VALUES FROM (%L) TO (%L)',
                v_name, v_start, v_end);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- API access log (even higher volume, shorter retention)
-- -----------------------------------------------------------------------------
CREATE TABLE api_audit_log (
    id              BIGSERIAL,
    organization_id UUID,
    method          TEXT NOT NULL,
    path            TEXT NOT NULL,
    status_code     INT,
    duration_ms     INT,
    actor_id        UUID,
    request_id      TEXT,
    client_ip       INET,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

COMMENT ON TABLE api_audit_log IS 'HTTP access log produced by the API gateway. Short retention (90 days), partitioned by month.';

CREATE OR REPLACE FUNCTION audit_create_api_log_partitions(p_lookahead_months INT DEFAULT 4)
RETURNS VOID AS $$
DECLARE
    v_month DATE;
    v_start TEXT;
    v_end TEXT;
    v_name TEXT;
BEGIN
    v_month := date_trunc('month', current_date);
    FOR i IN 0..(p_lookahead_months - 1) LOOP
        v_name := audit_partition_name('api_audit_log', (v_month + (i * interval '1 month'))::date);
        v_start := to_char(date_trunc('month', v_month + (i * interval '1 month')), 'YYYY-MM-DD');
        v_end := to_char(date_trunc('month', v_month + ((i + 1) * interval '1 month')), 'YYYY-MM-DD');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF api_audit_log FOR VALUES FROM (%L) TO (%L)',
                v_name, v_start, v_end);
            EXECUTE format(
                'CREATE INDEX %I ON %I (organization_id, occurred_at DESC)',
                v_name || '_org_time', v_name);
            EXECUTE format(
                'CREATE INDEX %I ON %I (method, path, occurred_at DESC)',
                v_name || '_method_path', v_name);
            EXECUTE format(
                'CREATE INDEX %I ON %I (request_id)',
                v_name || '_request', v_name);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- Index strategy (non-partitioned)
-- -----------------------------------------------------------------------------
CREATE INDEX idx_audit_event_changes_event ON audit_event_changes (audit_event_id);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_audit_events ON audit_events
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_api_audit_log ON api_audit_log
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

SELECT audit_create_partitions(3);
SELECT audit_create_change_partitions(3);
SELECT audit_create_api_log_partitions(4);
