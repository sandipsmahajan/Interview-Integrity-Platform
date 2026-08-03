-- Fixes a schema defect from V1: the audit trigger on reports references
-- NEW.updated_by, but the reports table never declared an updated_by column,
-- so every INSERT/UPDATE on reports failed with
-- 'record "new" has no field "updated_by"'.
--
-- This migration adds the missing column and recreates the trigger with a
-- meaningful actor fallback chain (updated_by -> requested_by -> app.user_id).

ALTER TABLE reports ADD COLUMN updated_by UUID;

COMMENT ON COLUMN reports.updated_by IS 'Actor who last updated the report. Falls back to requested_by or the configured app.user_id in audit history.';

CREATE OR REPLACE FUNCTION audit_reports_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO reports_history(history_action, changed_by, id, organization_id, type, title,
                                status, format, score, version)
    VALUES (TG_OP, COALESCE(NEW.updated_by, NEW.requested_by,
                            current_setting('app.user_id', true)::uuid),
            NEW.id, NEW.organization_id, NEW.type, NEW.title, NEW.status, NEW.format,
            NEW.score, NEW.version);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
