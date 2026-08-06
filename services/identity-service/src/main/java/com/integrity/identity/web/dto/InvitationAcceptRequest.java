package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to accept an invitation and set an initial password.
 *
 * @param token invitation purpose token
 * @param newPassword the user's chosen password
 */
public record InvitationAcceptRequest(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
