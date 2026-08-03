-- Hardens the "one active monitoring session per interview run" invariant
-- documented on interview_sessions. InterviewSessionService.start() guards the
-- insert with a check-then-act query, which can race under concurrency and
-- produce two ACTIVE/PAUSED sessions for the same interview.
--
-- A partial unique index over live sessions makes the invariant a database
-- guarantee: the second concurrent start now fails with a duplicate key
-- violation instead of silently creating a duplicate monitoring session.

CREATE UNIQUE INDEX uq_interview_sessions_active
    ON interview_sessions (interview_id)
    WHERE status IN ('ACTIVE', 'PAUSED');
