-- =============================================================================
-- telemetry_db - High-volume telemetry time series (billions of events)
-- Owning service: telemetry-service
--
-- Design goals
--   * telemetry_events is RANGE partitioned by month on occurred_at
--   * every partition owns its indexes; the BRIN index serves range scans,
--     the GIN index serves JSONB payload searches
--   * retention: old partitions are detached (archive) then dropped by policy
--   * hourly rollups in telemetry_event_summaries answer fast time-window queries
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE telemetry_session_status AS ENUM ('STARTED', 'ACTIVE', 'ENDED', 'ABANDONED');

-- -----------------------------------------------------------------------------
-- Functions (shared conventions)
-- -----------------------------------------------------------------------------
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
-- Reference tables (global)
-- -----------------------------------------------------------------------------
CREATE TABLE telemetry_event_types (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code           TEXT NOT NULL,
    name           TEXT NOT NULL,
    description    TEXT,
    retention_days INT NOT NULL DEFAULT 730,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_telemetry_event_types_code UNIQUE (code),
    CONSTRAINT chk_retention_days CHECK (retention_days > 0)
);

COMMENT ON TABLE telemetry_event_types IS 'Global catalog of telemetry event types with per-type retention.';

-- -----------------------------------------------------------------------------
-- Master table
-- -----------------------------------------------------------------------------
CREATE TABLE telemetry_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID NOT NULL,
    interview_id            UUID NOT NULL,
    candidate_id            UUID,
    device_id               TEXT,
    client_version          TEXT,
    status                  telemetry_session_status NOT NULL DEFAULT 'STARTED',
    heartbeat_cadence_seconds INT NOT NULL DEFAULT 5,
    started_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at                TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_telemetry_heartbeat_cadence CHECK (heartbeat_cadence_seconds BETWEEN 1 AND 3600)
);

COMMENT ON TABLE telemetry_sessions IS 'Monitoring session master. interview_id soft-references interview_db.interview_sessions.';

-- -----------------------------------------------------------------------------
-- Partitioned transaction table (billions of rows)
-- -----------------------------------------------------------------------------
CREATE TABLE telemetry_events (
    id                 UUID NOT NULL DEFAULT gen_random_uuid(),
    organization_id    UUID NOT NULL,
    session_id         UUID NOT NULL,
    interview_id       UUID,
    event_type         TEXT NOT NULL,
    seq                BIGINT NOT NULL,
    occurred_at        TIMESTAMPTZ NOT NULL,
    client_occurred_at TIMESTAMPTZ,
    payload            JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

COMMENT ON TABLE telemetry_events IS 'Raw telemetry events. Partitioned by month on occurred_at. PK includes the partition key because Postgres requires partition columns in unique indexes.';

-- -----------------------------------------------------------------------------
-- Partition lifecycle management
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION telemetry_partition_name(p_month DATE)
RETURNS TEXT AS $$
BEGIN
    RETURN 'telemetry_events_' || to_char(p_month, 'YYYY_MM');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION telemetry_create_partition(p_month DATE)
RETURNS TEXT AS $$
DECLARE
    v_name TEXT;
    v_start TEXT;
    v_end TEXT;
BEGIN
    v_name := telemetry_partition_name(p_month);
    v_start := to_char(date_trunc('month', p_month), 'YYYY-MM-DD');
    v_end := to_char(date_trunc('month', p_month) + interval '1 month', 'YYYY-MM-DD');
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF telemetry_events '
            'FOR VALUES FROM (%L) TO (%L)',
            v_name, v_start, v_end);
        -- BRIN index for time-window range scans (cheap to maintain under inserts)
        EXECUTE format(
            'CREATE INDEX %I ON %I USING BRIN (occurred_at, seq) WITH (pages_per_range = 128)',
            v_name || '_occurred_brin', v_name);
        -- GIN index for JSONB payload searches within the partition
        EXECUTE format(
            'CREATE INDEX %I ON %I USING GIN (payload jsonb_path_ops)',
            v_name || '_payload_gin', v_name);
        -- session + event_type covering index for the most common analytic path
        EXECUTE format(
            'CREATE INDEX %I ON %I (session_id, event_type, seq) INCLUDE (payload)',
            v_name || '_session_type_idx', v_name);
        EXECUTE format(
            'CREATE INDEX %I ON %I (organization_id, occurred_at)',
            v_name || '_org_time_idx', v_name);
        EXECUTE format('ANALYZE %I', v_name);
    END IF;
    RETURN v_name;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION telemetry_ensure_partitions(p_lookahead_months INT DEFAULT 3, p_anchor DATE DEFAULT current_date)
RETURNS VOID AS $$
DECLARE
    v_month DATE;
BEGIN
    v_month := date_trunc('month', p_anchor);
    FOR i IN 0..(p_lookahead_months - 1) LOOP
        PERFORM telemetry_create_partition((v_month + (i * interval '1 month'))::date);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION telemetry_drop_partition_before(p_before_month DATE)
RETURNS VOID AS $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT c.relname AS child
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
        JOIN pg_class p ON p.oid = i.inhparent
        WHERE p.relname = 'telemetry_events'
    LOOP
        IF to_date(substr(rec.child, length('telemetry_events_') + 1), 'YYYY_MM') < date_trunc('month', p_before_month) THEN
            EXECUTE format('DROP TABLE IF EXISTS %I', rec.child);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Archive a partition: detach it from the partitioned parent so the raw data
-- can be moved to cold storage or an archive schema without blocking inserts.
CREATE OR REPLACE FUNCTION telemetry_archive_partition(p_month DATE)
RETURNS TEXT AS $$
DECLARE
    v_name TEXT;
BEGIN
    v_name := telemetry_partition_name(p_month);
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
        EXECUTE format('ALTER TABLE telemetry_events DETACH PARTITION %I', v_name);
    END IF;
    RETURN v_name;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- Hourly rollups (aggregation table for fast time-window analytics)
-- -----------------------------------------------------------------------------
CREATE TABLE telemetry_event_summaries (
    bucket_start     TIMESTAMPTZ NOT NULL,
    bucket_end       TIMESTAMPTZ NOT NULL,
    organization_id  UUID NOT NULL,
    session_id       UUID NOT NULL,
    event_type       TEXT NOT NULL,
    event_count      BIGINT NOT NULL DEFAULT 0,
    min_seq          BIGINT,
    max_seq          BIGINT,
    last_payload     JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (bucket_start, organization_id, session_id, event_type)
) PARTITION BY RANGE (bucket_start);

COMMENT ON TABLE telemetry_event_summaries IS 'Hourly per-session event rollups enabling fast dashboards without scanning raw events.';

-- Summaries are bucketed by month to keep partitions small.
CREATE OR REPLACE FUNCTION telemetry_summary_partition_name(p_month DATE)
RETURNS TEXT AS $$
BEGIN
    RETURN 'telemetry_event_summaries_' || to_char(p_month, 'YYYY_MM');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION telemetry_ensure_summary_partitions(p_lookahead_months INT DEFAULT 3)
RETURNS VOID AS $$
DECLARE
    v_month DATE;
    v_name TEXT;
    v_start TEXT;
    v_end TEXT;
BEGIN
    v_month := date_trunc('month', current_date);
    FOR i IN 0..(p_lookahead_months - 1) LOOP
        v_name := telemetry_summary_partition_name((v_month + (i * interval '1 month'))::date);
        v_start := to_char(date_trunc('month', v_month + (i * interval '1 month')), 'YYYY-MM-DD');
        v_end := to_char(date_trunc('month', v_month + ((i + 1) * interval '1 month')), 'YYYY-MM-DD');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = v_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF telemetry_event_summaries '
                'FOR VALUES FROM (%L) TO (%L)', v_name, v_start, v_end);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Rollup worker: aggregates the previous hour from raw events (idempotent via
-- upsert on the primary key). Scheduled by scheduler-service or pg_cron.
CREATE OR REPLACE FUNCTION telemetry_rollup_hour(p_bucket TIMESTAMPTZ)
RETURNS BIGINT AS $$
DECLARE
    v_rows BIGINT;
BEGIN
    INSERT INTO telemetry_event_summaries
        (bucket_start, bucket_end, organization_id, session_id, event_type,
         event_count, min_seq, max_seq, last_payload)
    SELECT
        date_trunc('hour', p_bucket),
        date_trunc('hour', p_bucket) + interval '1 hour',
        organization_id, session_id, event_type,
        count(*), min(seq), max(seq),
        (ARRAY_AGG(payload ORDER BY seq DESC))[1]
    FROM telemetry_events
    WHERE occurred_at >= date_trunc('hour', p_bucket)
      AND occurred_at < date_trunc('hour', p_bucket) + interval '1 hour'
    GROUP BY organization_id, session_id, event_type
    ON CONFLICT (bucket_start, organization_id, session_id, event_type)
    DO UPDATE SET
        event_count = EXCLUDED.event_count,
        min_seq = EXCLUDED.min_seq,
        max_seq = EXCLUDED.max_seq,
        last_payload = EXCLUDED.last_payload,
        updated_at = now();
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    RETURN v_rows;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- Views
-- -----------------------------------------------------------------------------
CREATE VIEW v_telemetry_session_counts AS
SELECT session_id,
       count(*)                         AS event_count,
       min(occurred_at)                 AS first_seen,
       max(occurred_at)                 AS last_seen,
       count(*) FILTER (WHERE event_type = 'HEARTBEAT') AS heartbeat_count
FROM telemetry_events
GROUP BY session_id;

CREATE MATERIALIZED VIEW mv_telemetry_event_type_stats AS
SELECT event_type,
       count(*)                     AS total_events,
       min(occurred_at)             AS earliest,
       max(occurred_at)             AS latest,
       count(DISTINCT organization_id) AS tenant_count
FROM telemetry_events
GROUP BY event_type
WITH NO DATA;

COMMENT ON MATERIALIZED VIEW mv_telemetry_event_type_stats IS 'Cross-tenant event type statistics. Refresh with REFRESH MATERIALIZED VIEW; refresh is scheduled by scheduler-service.';

-- -----------------------------------------------------------------------------
-- Index strategy (non-partitioned tables)
-- -----------------------------------------------------------------------------
CREATE INDEX idx_telemetry_sessions_org ON telemetry_sessions (organization_id, started_at DESC);
CREATE INDEX idx_telemetry_sessions_interview ON telemetry_sessions (interview_id);
CREATE INDEX idx_telemetry_sessions_active ON telemetry_sessions (status) WHERE status IN ('STARTED', 'ACTIVE');

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE telemetry_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE telemetry_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE telemetry_event_summaries ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_telemetry_sessions ON telemetry_sessions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_telemetry_events ON telemetry_events
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_telemetry_event_summaries ON telemetry_event_summaries
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

-- Ensure the current and next months exist so inserts never fail at runtime.
SELECT telemetry_ensure_partitions(3);
SELECT telemetry_ensure_summary_partitions(3);
