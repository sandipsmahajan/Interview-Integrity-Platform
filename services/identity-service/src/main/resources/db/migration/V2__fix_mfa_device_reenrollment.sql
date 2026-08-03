-- Fixes a schema defect from V1: the table constraint uq_mfa_user_kind
-- UNIQUE(user_id, kind) blocks re-enrollment after a device is soft-deleted.
-- MfaService.removeDevice sets deleted_at instead of removing the row, so a
-- later enrollTotp inserts a new device with the same (user_id, kind) and hits
-- the still-present constraint, making TOTP re-enrollment impossible after a
-- device removal.
--
-- The table constraint is replaced by a partial unique index that only applies
-- to live rows, so soft-deleted devices no longer block new enrollments.

ALTER TABLE mfa_devices DROP CONSTRAINT uq_mfa_user_kind;

CREATE UNIQUE INDEX uq_mfa_user_kind_live ON mfa_devices (user_id, kind) WHERE deleted_at IS NULL;
