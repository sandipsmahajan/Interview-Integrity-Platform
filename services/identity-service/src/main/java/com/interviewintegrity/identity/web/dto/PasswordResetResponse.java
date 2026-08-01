package com.interviewintegrity.identity.web.dto;

/**
 * Result of a password reset request.
 *
 * <p>In a production deployment the token is delivered by email; this response exposes it for local
 * development and API clients.
 *
 * @param resetToken one-time password reset token
 * @param expiresInSeconds token lifetime in seconds
 */
public record PasswordResetResponse(String resetToken, long expiresInSeconds) {}
