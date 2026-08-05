package com.integrity.identity.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public profile of a user.
 *
 * @param id user identifier
 * @param organizationId tenant identifier
 * @param email user email address
 * @param displayName display name
 * @param status account lifecycle status
 * @param emailVerifiedAt instant the email was verified, null when unverified
 * @param lastLoginAt instant of the last successful login, null when never
 * @param createdAt instant the account was created
 * @param roles role codes assigned to the user
 */
public record UserResponse(
    UUID id,
    UUID organizationId,
    String email,
    String displayName,
    String status,
    Instant emailVerifiedAt,
    Instant lastLoginAt,
    Instant createdAt,
    List<String> roles) {}
