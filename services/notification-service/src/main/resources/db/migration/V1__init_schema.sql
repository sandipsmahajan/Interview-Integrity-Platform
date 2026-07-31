-- =============================================================================
-- notification_db - Notifications, templates, preferences and deliveries
-- Owning service: notification-service
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH', 'IN_APP', 'WEBHOOK');

CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED');

CREATE TYPE notification_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'URGENT');

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
-- Master / reference tables
-- -----------------------------------------------------------------------------
CREATE TABLE notification_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID, -- NULL = platform default template
    code            TEXT NOT NULL,
    channel         notification_channel NOT NULL DEFAULT 'EMAIL',
    subject         TEXT,
    body_template   TEXT NOT NULL,
    locale          TEXT NOT NULL DEFAULT 'en',
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1
);

COMMENT ON TABLE notification_templates IS 'Message templates. organization_id NULL provides platform defaults; tenants can override by code + channel + locale.';

CREATE UNIQUE INDEX uq_notification_templates_org_code
    ON notification_templates (organization_id, code, channel, locale) WHERE deleted_at IS NULL;

CREATE TABLE notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    channel         notification_channel NOT NULL DEFAULT 'EMAIL',
    notification_type TEXT NOT NULL DEFAULT 'ALL',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_notification_preferences UNIQUE (user_id, channel, notification_type)
);

COMMENT ON TABLE notification_preferences IS 'Per-user opt-in/opt-out for notification channels and types.';

-- -----------------------------------------------------------------------------
-- Transaction tables
-- -----------------------------------------------------------------------------
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id         UUID NOT NULL,
    notification_type TEXT NOT NULL,
    channel         notification_channel NOT NULL,
    subject         TEXT,
    body            TEXT NOT NULL,
    priority        notification_priority NOT NULL DEFAULT 'MEDIUM',
    status          notification_status NOT NULL DEFAULT 'PENDING',
    scheduled_at    TIMESTAMPTZ,
    sent_at         TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_notifications_body_not_blank CHECK (length(btrim(body)) > 0)
);

COMMENT ON TABLE notifications IS 'Outbound notification records. user_id soft-references identity_db.users. Designed for high insert volume.';

CREATE TABLE notification_deliveries (
    id                 BIGSERIAL PRIMARY KEY,
    notification_id    UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel            notification_channel NOT NULL,
    provider           TEXT NOT NULL,
    provider_message_id TEXT,
    status             notification_status NOT NULL DEFAULT 'PENDING',
    attempts           INT NOT NULL DEFAULT 0,
    last_error         TEXT,
    sent_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE notification_deliveries IS 'Delivery attempt history per notification (1 notification : N deliveries).';

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_org_status ON notifications (organization_id, status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_org_time ON notifications (organization_id, created_at DESC);
CREATE INDEX idx_notifications_read ON notifications (user_id) WHERE read_at IS NULL;

CREATE INDEX idx_notification_deliveries_notification ON notification_deliveries (notification_id);
CREATE INDEX idx_notification_deliveries_provider ON notification_deliveries (provider, provider_message_id);

CREATE INDEX idx_notification_preferences_user ON notification_preferences (user_id);

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_deliveries ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_notifications ON notifications
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_notification_preferences ON notification_preferences
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_notification_deliveries ON notification_deliveries
    USING (EXISTS (SELECT 1 FROM notifications n WHERE n.id = notification_id AND n.organization_id = current_tenant_id()))
    WITH CHECK (EXISTS (SELECT 1 FROM notifications n WHERE n.id = notification_id AND n.organization_id = current_tenant_id()));
