-- =============================================================================
-- telemetry_db - Repeatable reference data (event types)
-- Idempotent: adds missing rows only.
-- =============================================================================

INSERT INTO telemetry_event_types (code, name, description, retention_days)
SELECT v.code, v.name, v.description, v.retention_days
FROM (VALUES
    ('HEARTBEAT',       'Heartbeat',             'Periodic liveness signal from the client', 180),
    ('TAB_SWITCH',      'Tab switch',            'The interview tab lost focus', 730),
    ('WINDOW_BLUR',     'Window blur',           'The secured window lost focus', 730),
    ('WINDOW_RESIZE',   'Window resize',         'The browser window was resized', 730),
    ('KEY_PRESS',       'Key press',             'Keyboard activity', 90),
    ('MOUSE_CLICK',     'Mouse click',           'Mouse activity', 90),
    ('PASTE_EVENT',     'Paste event',           'Text was pasted into the interview surface', 730),
    ('COPY_EVENT',      'Copy event',            'Text was copied from the interview surface', 730),
    ('SCREENSHOT',      'Screenshot',            'Screen capture uploaded by the client', 90),
    ('SYSTEM_CHECK',    'System check',          'Pre-interview system/VM/device audit result', 365),
    ('NETWORK_CHANGE',  'Network change',        'Network identity or connectivity change', 730),
    ('PROCESS_SCAN',    'Process scan',          'Unauthorized process activity report', 730),
    ('PROCTOR_ALERT',   'Proctor alert',         'Client-side rule trigger, awaiting policy verdict', 365),
    ('AUDIO_LEVEL',     'Audio level',           'Microphone activity samples', 30),
    ('VIDEO_LEVEL',     'Video level',           'Camera activity samples', 30),
    ('EXAM_NAVIGATION', 'Exam navigation',       'Navigation within the secured interview surface', 365),
    ('DEVICE_INFO',     'Device info',           'Client device fingerprint payload', 180)
) AS v(code, name, description, retention_days)
WHERE NOT EXISTS (SELECT 1 FROM telemetry_event_types t WHERE t.code = v.code);
