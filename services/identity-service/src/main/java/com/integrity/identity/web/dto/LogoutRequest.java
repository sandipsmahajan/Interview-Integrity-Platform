package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to terminate a session.
 *
 * @param refreshToken the refresh token of the session to revoke
 */
public record LogoutRequest(@NotBlank String refreshToken) {}
