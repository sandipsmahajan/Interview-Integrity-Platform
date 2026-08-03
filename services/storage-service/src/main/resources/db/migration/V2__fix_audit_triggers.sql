-- Fixes a schema defect from V1: the audit trigger on storage_objects references
-- NEW.updated_by, but the storage_objects table never declared an updated_by
-- column, so every INSERT/UPDATE on storage_objects failed with
-- 'record "new" has no field "updated_by"'.
--
-- This migration adds the missing column and recreates the trigger with a
-- meaningful actor fallback chain (updated_by -> uploaded_by -> app.user_id).

ALTER TABLE storage_objects ADD COLUMN updated_by UUID;

COMMENT ON COLUMN storage_objects.updated_by IS 'Actor who last updated the object. Falls back to uploaded_by or the configured app.user_id in audit history.';

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
