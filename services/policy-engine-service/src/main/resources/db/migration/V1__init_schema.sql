-- =============================================================================
-- policy_db - Integrity policies, rules and violations
-- Owning service: policy-engine-service
-- Policies are versioned and evaluated against telemetry events; detected
-- violations are triaged through review/escalation workflows.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE policy_status AS ENUM ('DRAFT', 'ACTIVE', 'ARCHIVED');

CREATE TYPE violation_severity AS ENUM ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

CREATE TYPE violation_status AS ENUM ('OPEN', 'IN_REVIEW', 'ESCALATED', 'RESOLVED', 'DISMISSED');

CREATE TYPE review_action AS ENUM ('CONFIRM', 'DISMISS', 'ESCALATE');

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
CREATE TABLE policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    code            TEXT NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    status          policy_status NOT NULL DEFAULT 'DRAFT',
    default_severity violation_severity NOT NULL DEFAULT 'MEDIUM',
    priority        INT NOT NULL DEFAULT 100,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_policies_code_not_blank CHECK (length(btrim(code)) > 0),
    CONSTRAINT chk_policies_priority CHECK (priority >= 0)
);

COMMENT ON TABLE policies IS 'Tenant scoped integrity policies. One policy groups a set of rules.';

CREATE TABLE policy_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    policy_id       UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    rule_code       TEXT NOT NULL,
    description     TEXT,
    -- condition is a JSONB predicate, e.g.
    -- {"eventType": "HEARTBEAT", "op": "gt", "field": "secondsSincePreviousHeartbeat", "value": 10}
    condition       JSONB NOT NULL,
    severity        violation_severity NOT NULL DEFAULT 'MEDIUM',
    weight          INT NOT NULL DEFAULT 1,
    order_index     INT NOT NULL DEFAULT 0,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_policy_rules_code_not_blank CHECK (length(btrim(rule_code)) > 0),
    CONSTRAINT chk_policy_rules_weight CHECK (weight >= 0)
);

COMMENT ON TABLE policy_rules IS 'Evaluable rules belonging to a policy. condition holds the JSON predicate evaluated against telemetry events.';

CREATE TABLE policy_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    policy_id       UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    version         INT NOT NULL,
    definition      JSONB NOT NULL,
    status          policy_status NOT NULL DEFAULT 'DRAFT',
    published_by    UUID,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_policy_versions UNIQUE (policy_id, version)
);

COMMENT ON TABLE policy_versions IS 'Immutable snapshot of a policy definition at each version.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE violations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL,
    session_id        UUID NOT NULL,
    interview_id      UUID,
    policy_id         UUID REFERENCES policies(id),
    rule_code         TEXT NOT NULL,
    severity          violation_severity NOT NULL,
    message           TEXT,
    status            violation_status NOT NULL DEFAULT 'OPEN',
    evidence          JSONB,
    occurred_at       TIMESTAMPTZ NOT NULL,
    detected_by       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_violations_rule_code_not_blank CHECK (length(btrim(rule_code)) > 0)
);

COMMENT ON TABLE violations IS 'Detected integrity violations. session_id soft-references telemetry_db.telemetry_sessions. Designed for millions of rows.';

CREATE TABLE violation_reviews (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    violation_id  UUID NOT NULL REFERENCES violations(id) ON DELETE CASCADE,
    reviewer_id   UUID NOT NULL,
    action        review_action NOT NULL,
    comment       TEXT,
    reviewed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE violation_reviews IS 'Human review decisions on violations. reviewer_id soft-references identity_db.users.';

CREATE TABLE violation_escalations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    violation_id  UUID NOT NULL REFERENCES violations(id) ON DELETE CASCADE,
    escalated_to  UUID NOT NULL,
    reason        TEXT,
    escalated_by  UUID,
    escalated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at   TIMESTAMPTZ,
    resolution    TEXT
);

COMMENT ON TABLE violation_escalations IS 'Escalation of violations to senior reviewers.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE policies_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    code            TEXT,
    name            TEXT,
    status          policy_status,
    default_severity violation_severity,
    priority        INT,
    enabled         BOOLEAN,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_policies_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO policies_history(history_action, changed_by, id, organization_id, code, name,
                                     status, default_severity, priority, enabled, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.organization_id, OLD.code, OLD.name, OLD.status, OLD.default_severity,
                OLD.priority, OLD.enabled, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO policies_history(history_action, changed_by, id, organization_id, code, name,
                                     status, default_severity, priority, enabled, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.organization_id, NEW.code, NEW.name, NEW.status, NEW.default_severity,
                NEW.priority, NEW.enabled, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_policies_history
    AFTER INSERT OR UPDATE OR DELETE ON policies
    FOR EACH ROW EXECUTE FUNCTION audit_policies_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_policies_org_code ON policies (organization_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_org_status ON policies (organization_id, status, enabled) WHERE deleted_at IS NULL;

CREATE INDEX idx_policy_rules_policy ON policy_rules (policy_id, enabled) WHERE deleted_at IS NULL;
CREATE INDEX idx_policy_rules_org ON policy_rules (organization_id, rule_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_policy_rules_condition ON policy_rules USING GIN (condition jsonb_path_ops);

CREATE INDEX idx_policy_versions_policy ON policy_versions (policy_id, version DESC);

-- Violation query patterns: triage queue, session drill-down, interview summary
CREATE INDEX idx_violations_org_status ON violations (organization_id, status, occurred_at DESC) WHERE status != 'DISMISSED';
CREATE INDEX idx_violations_org_time ON violations (organization_id, occurred_at DESC);
CREATE INDEX idx_violations_session ON violations (session_id, occurred_at ASC);
CREATE INDEX idx_violations_interview ON violations (interview_id, severity);
CREATE INDEX idx_violations_rule ON violations (rule_code, occurred_at DESC);
CREATE INDEX idx_violations_evidence ON violations USING GIN (evidence jsonb_path_ops);

CREATE INDEX idx_violation_reviews_violation ON violation_reviews (violation_id, reviewed_at DESC);
CREATE INDEX idx_violation_escalations_violation ON violation_escalations (violation_id) WHERE resolved_at IS NULL;
CREATE INDEX idx_violation_escalations_to ON violation_escalations (escalated_to) WHERE resolved_at IS NULL;

-- -----------------------------------------------------------------------------
-- Views
-- -----------------------------------------------------------------------------
CREATE VIEW v_open_violations AS
SELECT organization_id, session_id, interview_id, rule_code, severity, status, occurred_at
FROM violations
WHERE status IN ('OPEN', 'IN_REVIEW', 'ESCALATED');

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE policy_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE policy_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE violations ENABLE ROW LEVEL SECURITY;
ALTER TABLE violation_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE violation_escalations ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policies ON policies
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy_rules ON policy_rules
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_policy_versions ON policy_versions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_violations ON violations
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_violation_reviews ON violation_reviews
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_violation_escalations ON violation_escalations
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
