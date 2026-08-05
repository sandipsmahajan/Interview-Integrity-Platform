package com.integrity.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request to create a new user, typically by invitation.
 *
 * @param email email of the new user
 * @param displayName display name of the new user
 * @param roleIds roles to assign on creation
 */
public record CreateUserRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(max = 120) String displayName,
    List<UUID> roleIds) {}
