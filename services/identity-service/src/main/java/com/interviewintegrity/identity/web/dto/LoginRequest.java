package com.interviewintegrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to authenticate a user.
 *
 * @param email user email address
 * @param password plain text password
 * @param organizationId optional tenant hint, required when the email exists in several
 *     organizations
 * @param deviceId optional stable device identifier for the session
 * @param userAgent optional client user agent captured by the caller
 */
public record LoginRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 128) String password,
    UUID organizationId,
    @Size(max = 200) String deviceId,
    @Size(max = 500) String userAgent) {}
