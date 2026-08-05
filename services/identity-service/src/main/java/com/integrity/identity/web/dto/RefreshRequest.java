package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request carrying an opaque refresh token.
 *
 * @param refreshToken the refresh token previously issued during authentication
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
