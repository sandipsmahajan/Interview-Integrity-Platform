package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update the mutable profile fields of a user.
 *
 * @param displayName new display name
 */
public record UpdateUserRequest(@NotBlank @Size(max = 120) String displayName) {}
