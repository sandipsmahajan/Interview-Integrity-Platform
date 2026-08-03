-- =============================================================================
-- notification_db - V2: email recipient + platform default email templates
-- Owning service: notification-service
-- =============================================================================

-- The recipient address is persisted so the retry worker can re-dispatch a
-- pending email without re-reading the source event.
ALTER TABLE notifications ADD COLUMN recipient TEXT;

COMMENT ON COLUMN notifications.recipient IS 'Recipient address for email dispatch; null for non-email channels.';

CREATE INDEX idx_notifications_email_pending
    ON notifications (channel, status, created_at)
    WHERE channel = 'EMAIL' AND status = 'PENDING';

-- -----------------------------------------------------------------------------
-- Platform default email templates (organization_id NULL).
-- Body templates use {{placeholder}} substitution from event templateData and
-- are wrapped by the dispatcher into a styled HTML message with a plaintext
-- fallback. The unique index in V1 keys on (organization_id, code, channel,
-- locale) so these single INSERTs stay idempotent per migration run.
-- -----------------------------------------------------------------------------
INSERT INTO notification_templates
    (organization_id, code, channel, subject, body_template, locale, is_default)
VALUES
    (NULL, 'email-verification', 'EMAIL',
     'Verify your email',
     '<p>Hi {{name}},</p><p>Welcome to {{appName}}. Please verify your email address by clicking the button below.</p><p><a href="{{verificationUrl}}">Verify email</a></p><p>If the button does not work, copy and paste this link into your browser:</p><p>{{verificationUrl}}</p><p>This link expires in {{expiresInMinutes}} minutes. If you did not create an account, you can safely ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'welcome', 'EMAIL',
     'Welcome to {{appName}}',
     '<p>Hi {{name}},</p><p>Your {{appName}} account is ready. We are excited to have you on board.</p><p>You can now sign in and start using the platform.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'password-reset', 'EMAIL',
     'Reset your password',
     '<p>Hi {{name}},</p><p>We received a request to reset your password. Click the button below to choose a new one.</p><p><a href="{{resetUrl}}">Reset password</a></p><p>If the button does not work, copy and paste this link into your browser:</p><p>{{resetUrl}}</p><p>This link expires in {{expiresInMinutes}} minutes. If you did not request a password reset, you can safely ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE),

    (NULL, 'email-otp', 'EMAIL',
     'Your one-time code',
     '<p>Hi {{name}},</p><p>Use the following one-time code to complete your sign-in:</p><p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{otpCode}}</p><p>This code expires in {{expiresInMinutes}} minutes. Never share it with anyone. If you did not request this code, please ignore this email.</p><p>— The {{appName}} team</p>',
     'en', TRUE);
