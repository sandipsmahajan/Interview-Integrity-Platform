-- =============================================================================
-- notification_db - Repeatable reference data (platform default email templates)
-- Idempotent: adds missing rows only.
-- organization_id NULL = platform default; tenants override by code + channel +
-- locale. The unique index in V1 keys on (organization_id, code, channel,
-- locale) WHERE deleted_at IS NULL, so the NOT EXISTS guard mirrors it.
-- =============================================================================

INSERT INTO notification_templates
    (organization_id, code, channel, subject, body_template, locale, is_default)
SELECT v.organization_id, v.code, v.channel, v.subject, v.body_template, v.locale, v.is_default
FROM (VALUES
    (NULL, 'email-verification', 'EMAIL'::notification_channel,
     'Verify your email',
     '<p>Hi {{name}},</p><p>Welcome to {{appName}}. Please verify your email address by clicking the button below.</p><p><a href="{{verificationUrl}}">Verify email</a></p><p>If the button does not work, copy and paste this link into your browser:</p><p>{{verificationUrl}}</p><p>This link expires in {{expiresInMinutes}} minutes. If you did not create an account, you can safely ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'welcome', 'EMAIL'::notification_channel,
     'Welcome to {{appName}}',
     '<p>Hi {{name}},</p><p>Your {{appName}} account is ready. We are excited to have you on board.</p><p>You can now sign in and start using the platform.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'password-reset', 'EMAIL'::notification_channel,
     'Reset your password',
     '<p>Hi {{name}},</p><p>We received a request to reset your password. Click the button below to choose a new one.</p><p><a href="{{resetUrl}}">Reset password</a></p><p>If the button does not work, copy and paste this link into your browser:</p><p>{{resetUrl}}</p><p>This link expires in {{expiresInMinutes}} minutes. If you did not request a password reset, you can safely ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'email-otp', 'EMAIL'::notification_channel,
     'Your one-time code',
     '<p>Hi {{name}},</p><p>Use the following one-time code to complete your sign-in:</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{otpCode}}</p><p>This code expires in {{expiresInMinutes}} minutes. Never share it with anyone. If you did not request this code, please ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE)
) AS v(organization_id, code, channel, subject, body_template, locale, is_default)
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates t
    WHERE t.organization_id IS NULL
      AND t.code = v.code
      AND t.channel = v.channel
      AND t.locale = v.locale
      AND t.deleted_at IS NULL
);
