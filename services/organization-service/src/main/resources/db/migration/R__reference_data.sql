-- =============================================================================
-- organization_db - Repeatable reference data (subscription plans)
-- Idempotent: adds missing rows only.
-- =============================================================================

INSERT INTO plans (code, name, monthly_price_cents, max_seats, features)
SELECT v.code, v.name, v.monthly_price_cents, v.max_seats, v.features::jsonb
FROM (VALUES
    ('starter',   'Starter',    0,      25, '{"interviews_per_month": 50, "report_types": ["SESSION"], "support": "community"}'),
    ('growth',    'Growth',     9900,   100, '{"interviews_per_month": 500, "report_types": ["SESSION", "CANDIDATE", "INTERVIEW"], "support": "standard"}'),
    ('enterprise','Enterprise', 49900,  NULL, '{"interviews_per_month": null, "report_types": ["SESSION", "CANDIDATE", "INTERVIEW", "RECRUITER", "ORGANIZATION", "INTEGRITY"], "support": "dedicated"}')
) AS v(code, name, monthly_price_cents, max_seats, features)
WHERE NOT EXISTS (SELECT 1 FROM plans p WHERE p.code = v.code);
