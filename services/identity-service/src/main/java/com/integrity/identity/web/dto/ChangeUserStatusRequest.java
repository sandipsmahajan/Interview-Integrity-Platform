package com.integrity.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to change the lifecycle status of a user.
 *
 * @param status one of ACTIVE, DISABLED, LOCKED
 */
public record ChangeUserStatusRequest(@NotBlank String status) {}
