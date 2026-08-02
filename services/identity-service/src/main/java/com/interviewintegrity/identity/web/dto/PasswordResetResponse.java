package com.interviewintegrity.identity.web.dto;

/**
 * Result of a password reset request.
 *
 * <p>The one-time token is always delivered by email; the response only exposes it when {@code
 * app.auth.expose-reset-token} is enabled (local development). Otherwise {@code resetToken} is
 * {@code null} so the response leaks nothing to unauthenticated callers.
 *
 * @param resetToken one-time password reset token, {@code null} unless exposure is enabled
 * @param expiresInSeconds token lifetime in seconds
 */
public record PasswordResetResponse(String resetToken, long expiresInSeconds) {}
