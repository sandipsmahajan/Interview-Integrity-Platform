package com.integrity.identity.web.dto;

/**
 * Token pair returned by successful authentication.
 *
 * @param accessToken signed JWT access token
 * @param refreshToken opaque refresh token
 * @param expiresInSeconds access token lifetime in seconds
 * @param tokenType token type discriminator
 * @param user profile of the authenticated user
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds,
    String tokenType,
    UserResponse user) {}
