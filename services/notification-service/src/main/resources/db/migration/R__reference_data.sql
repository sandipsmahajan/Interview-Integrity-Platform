-- =============================================================================
-- notification_db - Repeatable reference data (platform default email templates)
-- Idempotent: adds missing rows only.
-- organization_id NULL = platform default; tenants override by code + channel +
-- locale. The NOT EXISTS guard mirrors the partial unique index on
-- (organization_id, code, channel, locale) WHERE deleted_at IS NULL.
-- =============================================================================

INSERT INTO notification_templates
    (organization_id, code, channel, subject, body_template, locale, is_default)
SELECT v.organization_id, v.code, v.channel, v.subject, v.body_template, v.locale, v.is_default
FROM (VALUES
    (NULL::uuid, 'email-verification', 'EMAIL'::notification_channel,
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
     'en', TRUE),

    (NULL, 'interview-invitation', 'EMAIL'::notification_channel,
     'You have been invited to an interview — {{interviewTitle}}',
     '<div style="max-width:600px;font-family:Arial,sans-serif;color:#333">'
     || '<div style="background:#0f172a;padding:20px;text-align:center">'
     || '<h1 style="color:#fff;margin:0">{{appName}}</h1></div>'
     || '<div style="padding:24px;background:#fff">'
     || '<p>Hi {{candidateName}},</p>'
     || '<p>You have been invited to participate in the following interview:</p>'
     || '<table style="border-collapse:collapse;width:100%%;margin:16px 0">'
     || '<tr><td style="padding:8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold">Interview</td><td style="padding:8px;border:1px solid #e2e8f0">{{interviewTitle}}</td></tr>'
     || '<tr><td style="padding:8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold">Date &amp; Time</td><td style="padding:8px;border:1px solid #e2e8f0">{{interviewDate}}</td></tr>'
     || '<tr><td style="padding:8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold">Mode</td><td style="padding:8px;border:1px solid #e2e8f0">{{interviewMode}}</td></tr>'
     || '</table>'
     || '<p style="background:#fef9c3;padding:12px;border:1px solid #fde047;border-radius:4px">'
     || '<strong>Meeting Link:</strong> <a href="{{meetingUrl}}">{{meetingUrl}}</a></p>'
     || '<h3 style="margin-top:24px">Getting Started</h3>'
     || '<p><strong>1. Download the Integrity Pro desktop application:</strong></p>'
     || '<p><a href="{{downloadUrl}}" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:4px;font-weight:bold">Download Integrity Pro</a></p>'
     || '<p>Or copy and paste this link: {{downloadUrl}}</p>'
     || '<p><strong>2. Sign in with your temporary credentials:</strong></p>'
     || '<table style="border-collapse:collapse;width:100%%;margin:8px 0">'
     || '<tr><td style="padding:8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold">Email</td><td style="padding:8px;border:1px solid #e2e8f0">{{candidateEmail}}</td></tr>'
     || '<tr><td style="padding:8px;border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold">Temporary Password</td><td style="padding:8px;border:1px solid #e2e8f0;font-family:monospace;font-size:16px">{{tempPassword}}</td></tr>'
     || '</table>'
     || '<p style="color:#ef4444"><strong>Important:</strong> This password is temporary. You will be prompted to change it after your first sign-in.</p>'
     || '<h3 style="margin-top:24px">Before the Interview</h3>'
     || '<ul><li>Launch Integrity Pro at least 10 minutes before your scheduled time</li>'
     || '<li>Ensure your webcam and microphone are working</li>'
     || '<li>Close unnecessary applications</li>'
     || '<li>Find a quiet, well-lit space</li></ul>'
     || '<hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0">'
     || '<p style="color:#64748b;font-size:12px">This is an automated message from {{appName}}. If you have questions, please contact your recruiter.</p>'
     || '</div></div>',
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
