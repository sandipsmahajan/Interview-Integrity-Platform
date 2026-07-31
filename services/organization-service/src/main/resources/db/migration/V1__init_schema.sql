-- =============================================================================
-- organization_db - Tenant & organizational hierarchy
-- Owning service: organization-service
-- The organizations table is the TENANT ROOT: its id is the organization_id
-- referenced by every other database in the platform.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- ENUM types
-- -----------------------------------------------------------------------------
CREATE TYPE organization_status AS ENUM ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CLOSED');

CREATE TYPE subscription_status AS ENUM ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'UNPAID');

-- -----------------------------------------------------------------------------
-- Functions
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
CREATE TABLE plans (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               TEXT NOT NULL,
    name               TEXT NOT NULL,
    monthly_price_cents BIGINT NOT NULL DEFAULT 0,
    max_seats          INT,
    features           JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    version            BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT chk_plans_price CHECK (monthly_price_cents >= 0),
    CONSTRAINT chk_plans_max_seats CHECK (max_seats IS NULL OR max_seats > 0)
);

COMMENT ON TABLE plans IS 'Subscription plans offered by the platform (global catalog).';

-- -----------------------------------------------------------------------------
-- Tenant root
-- -----------------------------------------------------------------------------
CREATE TABLE organizations (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  TEXT NOT NULL,
    slug                  TEXT NOT NULL,
    legal_name            TEXT,
    status                organization_status NOT NULL DEFAULT 'TRIAL',
    settings              JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by            UUID,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by            UUID,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by            UUID,
    deleted_at            TIMESTAMPTZ,
    version               BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_organizations_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_organizations_slug_format
        CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    CONSTRAINT chk_organizations_version CHECK (version >= 0)
);

COMMENT ON TABLE organizations IS 'Tenant root. Every tenant-scoped row in every database references organizations.id as organization_id.';

CREATE TABLE organization_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    line1           TEXT,
    line2           TEXT,
    city            TEXT,
    region          TEXT,
    postal_code     TEXT,
    country_code    CHAR(2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE organization_addresses IS 'One-to-one billing/registered address per organization.';

CREATE TABLE organization_domains (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    domain          TEXT NOT NULL,
    verified_at     TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_domain_format CHECK (domain ~* '^[a-z0-9.-]+\.[a-z]{2,}$')
);

COMMENT ON TABLE organization_domains IS 'Email domains claimed by the tenant, used for SSO and auto-provisioning.';

CREATE TABLE subscriptions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id       UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    plan_id               UUID NOT NULL REFERENCES plans(id),
    status                subscription_status NOT NULL DEFAULT 'TRIALING',
    current_period_start  DATE NOT NULL,
    current_period_end    DATE NOT NULL,
    cancel_at_period_end  BOOLEAN NOT NULL DEFAULT FALSE,
    provider              TEXT,
    provider_subscription_id TEXT,
    created_by            UUID,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by            UUID,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_subscription_period CHECK (current_period_end >= current_period_start)
);

COMMENT ON TABLE subscriptions IS 'One active subscription per organization (1:1 enforced by unique organization_id).';

-- -----------------------------------------------------------------------------
-- Organizational hierarchy
-- -----------------------------------------------------------------------------
CREATE TABLE departments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    parent_id       UUID REFERENCES departments(id),
    name            TEXT NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_departments_name_not_blank CHECK (length(btrim(name)) > 0)
);

COMMENT ON TABLE departments IS 'Self-referencing department tree within a tenant.';

CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    department_id   UUID REFERENCES departments(id),
    name            TEXT NOT NULL,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE teams IS 'Teams that group users within a department.';

CREATE TABLE team_members (
    team_id     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    added_by    UUID,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (team_id, user_id)
);

COMMENT ON TABLE team_members IS 'Bridge between teams and users. user_id is a soft reference into identity_db (no FK across databases).';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE organizations_history (
    history_id     BIGSERIAL PRIMARY KEY,
    history_action TEXT NOT NULL,
    changed_by     UUID,
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    id             UUID NOT NULL,
    name           TEXT,
    slug           TEXT,
    legal_name     TEXT,
    status         organization_status,
    settings       JSONB,
    version        BIGINT
);

CREATE OR REPLACE FUNCTION audit_organizations_history()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO organizations_history(history_action, changed_by, id, name, slug, legal_name, status, settings, version)
        VALUES ('DELETE', COALESCE(OLD.deleted_by, current_setting('app.user_id', true)::uuid),
                OLD.id, OLD.name, OLD.slug, OLD.legal_name, OLD.status, OLD.settings, OLD.version);
        RETURN OLD;
    ELSE
        INSERT INTO organizations_history(history_action, changed_by, id, name, slug, legal_name, status, settings, version)
        VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
                NEW.id, NEW.name, NEW.slug, NEW.legal_name, NEW.status, NEW.settings, NEW.version);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_organizations_history
    AFTER INSERT OR UPDATE OR DELETE ON organizations
    FOR EACH ROW EXECUTE FUNCTION audit_organizations_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_organizations_slug ON organizations (slug) WHERE deleted_at IS NULL;

CREATE INDEX idx_organizations_status ON organizations (status) INCLUDE (name) WHERE deleted_at IS NULL;

CREATE INDEX idx_org_domains_org ON organization_domains (organization_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_org_domains_domain ON organization_domains (lower(domain)) WHERE deleted_at IS NULL;

CREATE INDEX idx_departments_parent ON departments (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_departments_org ON departments (organization_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_teams_org ON teams (organization_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_teams_department ON teams (department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_team_members_user ON team_members (user_id);

CREATE INDEX idx_subscriptions_org ON subscriptions (organization_id);
CREATE INDEX idx_subscriptions_plan ON subscriptions (plan_id, status);

CREATE INDEX idx_plans_code ON plans (code);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_domains ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_organizations ON organizations
    USING (id = current_tenant_id())
    WITH CHECK (id = current_tenant_id());

CREATE POLICY tenant_isolation_organization_addresses ON organization_addresses
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_organization_domains ON organization_domains
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_subscriptions ON subscriptions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_departments ON departments
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_teams ON teams
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_team_members ON team_members
    USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.organization_id = current_tenant_id()));
