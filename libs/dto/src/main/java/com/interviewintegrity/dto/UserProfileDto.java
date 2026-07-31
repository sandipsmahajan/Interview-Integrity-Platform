package com.interviewintegrity.dto;

import java.util.UUID;

/**
 * Public profile of a platform user shared across services.
 *
 * @param id user identifier
 * @param companyId owning company identifier
 * @param email user email address
 * @param displayName user display name
 */
public record UserProfileDto(UUID id, UUID companyId, String email, String displayName) {}
