-- =============================================================================
-- interview_db - Add candidate email and name to interviews
-- =============================================================================
ALTER TABLE interviews
    ADD COLUMN IF NOT EXISTS candidate_email TEXT,
    ADD COLUMN IF NOT EXISTS candidate_name TEXT;

COMMENT ON COLUMN interviews.candidate_email IS 'Email address of the interview candidate, used for notification delivery.';
COMMENT ON COLUMN interviews.candidate_name IS 'Display name of the interview candidate.';
