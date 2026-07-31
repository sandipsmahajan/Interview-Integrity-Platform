-- =============================================================================
-- integration_db - External integrations, connections, webhooks and sync logs
-- Owning service: integration-service
-- Credential material is stored as opaque ciphertext references; scopes and
-- connection state are tracked per external account.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE integration_status AS ENUM ('CONNECTED', 'DISCONNECTED', 'ERROR');

CREATE TYPE sync_direction AS ENUM ('INBOUND', 'OUTBOUND');

CREATE TYPE sync_status AS ENUM ('RUNNING', 'SUCCEEDED', 'FAILED');

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
CREATE TABLE integrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    provider        TEXT NOT NULL,
    name            TEXT NOT NULL,
    status          integration_status NOT NULL DEFAULT 'DISCONNECTED',
    credentials_ref TEXT NOT NULL,
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_integrations_provider_not_blank CHECK (length(btrim(provider)) > 0)
);

COMMENT ON TABLE integrations IS 'Integration definitions per tenant. credentials_ref points at the encrypted credential entry in storage/secret manager, never inline.';

CREATE TABLE integration_connections (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL,
    integration_id    UUID NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
    external_account_id TEXT NOT NULL,
    status            integration_status NOT NULL DEFAULT 'DISCONNECTED',
    scopes            TEXT[] NOT NULL DEFAULT '{}',
    connected_at      TIMESTAMPTZ,
    last_sync_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_integration_connections UNIQUE (integration_id, external_account_id)
);

COMMENT ON TABLE integration_connections IS 'Live connections to external accounts under an integration.';

CREATE TABLE integration_webhooks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    integration_id  UUID NOT NULL REFERENCES integrations(id) ON DELETE CASCADE,
    url             TEXT NOT NULL,
    secret_hash     TEXT NOT NULL,
    events          TEXT[] NOT NULL DEFAULT '{}',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_webhook_url CHECK (url ~ '^https://')
);

COMMENT ON TABLE integration_webhooks IS 'Outbound webhook subscriptions. secret_hash stores the HMAC signing secret hash.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE integration_sync_logs (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  UUID NOT NULL,
    connection_id    UUID NOT NULL REFERENCES integration_connections(id) ON DELETE CASCADE,
    direction        sync_direction NOT NULL,
    status           sync_status NOT NULL DEFAULT 'RUNNING',
    records_processed BIGINT NOT NULL DEFAULT 0,
    error_message    TEXT,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at      TIMESTAMPTZ,
    duration_ms      BIGINT
);

COMMENT ON TABLE integration_sync_logs IS 'Sync run history per connection.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE integrations_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    provider        TEXT,
    name            TEXT,
    status          integration_status,
    config          JSONB,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_integrations_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO integrations_history(history_action, changed_by, id, organization_id, provider,
                                     name, status, config, version)
    VALUES (TG_OP, COALESCE(NEW.updated_by, current_setting('app.user_id', true)::uuid),
            NEW.id, NEW.organization_id, NEW.provider, NEW.name, NEW.status, NEW.config, NEW.version);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_integrations_history
    AFTER INSERT OR UPDATE ON integrations
    FOR EACH ROW EXECUTE FUNCTION audit_integrations_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_integrations_org_provider ON integrations (organization_id, provider) WHERE deleted_at IS NULL;
CREATE INDEX idx_integrations_org ON integrations (organization_id, status) WHERE deleted_at IS NULL;

CREATE INDEX idx_integration_connections_integration ON integration_connections (integration_id);
CREATE INDEX idx_integration_connections_status ON integration_connections (status);

CREATE INDEX idx_integration_webhooks_integration ON integration_webhooks (integration_id, enabled);

CREATE INDEX idx_integration_sync_logs_connection ON integration_sync_logs (connection_id, started_at DESC);
CREATE INDEX idx_integration_sync_logs_org ON integration_sync_logs (organization_id, started_at DESC);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE integrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE integration_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE integration_webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE integration_sync_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_integrations ON integrations
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_integration_connections ON integration_connections
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_integration_webhooks ON integration_webhooks
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_integration_sync_logs ON integration_sync_logs
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
