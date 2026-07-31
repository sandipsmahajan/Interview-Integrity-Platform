-- =============================================================================
-- identity_db - Repeatable reference data
-- Idempotent: safe to run on every migration cycle. Only adds missing rows.
-- =============================================================================

INSERT INTO permissions (code, name, description, created_by)
SELECT v.code, v.name, v.description, NULL
FROM (VALUES
    ('identity.users.read',    'Read users',            'List and view platform users'),
    ('identity.users.write',   'Write users',           'Create, update and deactivate users'),
    ('identity.roles.manage',  'Manage roles',          'Create and assign RBAC roles'),
    ('identity.mfa.manage',    'Manage MFA',            'Register and revoke MFA devices'),
    ('interview.read',         'Read interviews',       'View interviews and sessions'),
    ('interview.write',        'Write interviews',      'Schedule, update and complete interviews'),
    ('candidate.read',         'Read candidates',       'View candidate profiles'),
    ('candidate.write',        'Write candidates',      'Create and update candidate profiles'),
    ('recruiter.read',         'Read recruiters',       'View recruiter profiles'),
    ('recruiter.write',        'Write recruiters',      'Create and update recruiter profiles'),
    ('telemetry.read',         'Read telemetry',        'View telemetry events and session data'),
    ('telemetry.write',        'Write telemetry',       'Ingest telemetry events'),
    ('policy.read',            'Read policies',         'View integrity policies and rules'),
    ('policy.write',           'Write policies',        'Create, update and publish policies'),
    ('policy.review',          'Review violations',     'Triage and resolve integrity violations'),
    ('report.read',            'Read reports',          'View generated reports'),
    ('report.write',           'Generate reports',      'Request and schedule reports'),
    ('analytics.read',         'Read analytics',        'View summaries and analytics dashboards'),
    ('audit.read',             'Read audit trail',      'View audit events'),
    ('storage.manage',         'Manage storage',        'Upload, manage and delete stored objects'),
    ('feature.manage',         'Manage feature flags',  'Control feature flags and rollouts'),
    ('config.manage',          'Manage configuration',  'Manage service and tenant configuration'),
    ('integration.manage',     'Manage integrations',   'Connect and manage external integrations')
) AS v(code, name, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code);
