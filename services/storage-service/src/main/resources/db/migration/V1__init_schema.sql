-- =============================================================================
-- storage_db - Object storage metadata (S3/MinIO compatible backend)
-- Owning service: storage-service
-- Tracks objects, buckets, versions and pre-signed URL grants. The binary
-- payload lives in the object store; this database holds the metadata.
-- Baseline      : squashed from the original development migrations
--                  V1__init_schema + V2__fix_audit_triggers (missing updated_by
--                  column added and the audit trigger repaired).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE storage_class AS ENUM ('STANDARD', 'INFREQUENT', 'ARCHIVE');

CREATE TYPE url_purpose AS ENUM ('UPLOAD', 'DOWNLOAD', 'DELETE');

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
CREATE TABLE storage_buckets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id   UUID NOT NULL,
    name              TEXT NOT NULL,
    versioning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    policy            JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by        UUID,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by        UUID,
    deleted_at        TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_bucket_name_format CHECK (name ~ '^[a-z0-9][a-z0-9._-]{1,62}$')
);

COMMENT ON TABLE storage_buckets IS 'Logical buckets owned by a tenant.';

CREATE TABLE storage_objects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    bucket_id       UUID NOT NULL REFERENCES storage_buckets(id) ON DELETE CASCADE,
    key             TEXT NOT NULL,
    size_bytes      BIGINT NOT NULL,
    content_type    TEXT,
    checksum_sha256 TEXT,
    storage_class   storage_class NOT NULL DEFAULT 'STANDARD',
    storage_ref     TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    uploaded_by     UUID,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      UUID,
    deleted_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_object_size CHECK (size_bytes >= 0),
    CONSTRAINT chk_checksum_format CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[a-f0-9]{64}$')
);

COMMENT ON TABLE storage_objects IS 'Metadata for every stored object. storage_ref points at the object-store key (S3/MinIO).';
COMMENT ON COLUMN storage_objects.updated_by IS 'Actor who last updated the object. Falls back to uploaded_by or the configured app.user_id in audit history.';

CREATE TABLE object_versions (
    id              BIGSERIAL PRIMARY KEY,
    object_id       UUID NOT NULL REFERENCES storage_objects(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    version         INT NOT NULL,
    storage_ref     TEXT NOT NULL,
    size_bytes      BIGINT NOT NULL,
    checksum_sha256 TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_object_versions UNIQUE (object_id, version)
);

COMMENT ON TABLE object_versions IS 'Immutable version history for versioned buckets.';

CREATE TABLE signed_urls (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    object_id       UUID NOT NULL REFERENCES storage_objects(id) ON DELETE CASCADE,
    purpose         url_purpose NOT NULL,
    token_hash      TEXT NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    max_uses        INT,
    usage_count     INT NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ,
    CONSTRAINT chk_signed_url_max_uses CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT chk_signed_url_usage CHECK (usage_count >= 0)
);

COMMENT ON TABLE signed_urls IS 'Pre-signed URL grants. token_hash stores the HMAC token; expiry and use limits enforced on every access.';

-- -----------------------------------------------------------------------------
-- History / audit
-- -----------------------------------------------------------------------------
CREATE TABLE storage_objects_history (
    history_id      BIGSERIAL PRIMARY KEY,
    history_action  TEXT NOT NULL,
    changed_by      UUID,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL,
    organization_id UUID NOT NULL,
    bucket_id       UUID,
    key             TEXT,
    size_bytes      BIGINT,
    content_type    TEXT,
    storage_class   storage_class,
    version         BIGINT
);

CREATE OR REPLACE FUNCTION audit_storage_objects_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO storage_objects_history(history_action, changed_by, id, organization_id, bucket_id,
                                        key, size_bytes, content_type, storage_class, version)
    VALUES (TG_OP, COALESCE(NEW.updated_by, NEW.uploaded_by,
                            current_setting('app.user_id', true)::uuid),
            NEW.id, NEW.organization_id, NEW.bucket_id, NEW.key, NEW.size_bytes,
            NEW.content_type, NEW.storage_class, NEW.version);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_storage_objects_history
    AFTER INSERT OR UPDATE ON storage_objects
    FOR EACH ROW EXECUTE FUNCTION audit_storage_objects_history();

-- -----------------------------------------------------------------------------
-- Index strategy
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_storage_buckets_org_name ON storage_buckets (organization_id, name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_storage_objects_bucket_key ON storage_objects (bucket_id, key) WHERE deleted_at IS NULL;
CREATE INDEX idx_storage_objects_org ON storage_objects (organization_id, uploaded_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_storage_objects_bucket ON storage_objects (bucket_id, uploaded_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_storage_objects_storage_class ON storage_objects (organization_id, storage_class) WHERE deleted_at IS NULL;
CREATE INDEX idx_storage_objects_metadata ON storage_objects USING GIN (metadata jsonb_path_ops);

CREATE INDEX idx_object_versions_object ON object_versions (object_id, version DESC);

CREATE INDEX idx_signed_urls_object ON signed_urls (object_id, purpose);
CREATE INDEX idx_signed_urls_token ON signed_urls (token_hash);
CREATE INDEX idx_signed_urls_expiry ON signed_urls (expires_at) WHERE revoked_at IS NULL;

-- -----------------------------------------------------------------------------
-- Multi-tenancy
-- -----------------------------------------------------------------------------
ALTER TABLE storage_buckets ENABLE ROW LEVEL SECURITY;
ALTER TABLE storage_objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE object_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE signed_urls ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_storage_buckets ON storage_buckets
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_storage_objects ON storage_objects
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_object_versions ON object_versions
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());

CREATE POLICY tenant_isolation_signed_urls ON signed_urls
    USING (organization_id = current_tenant_id())
    WITH CHECK (organization_id = current_tenant_id());
