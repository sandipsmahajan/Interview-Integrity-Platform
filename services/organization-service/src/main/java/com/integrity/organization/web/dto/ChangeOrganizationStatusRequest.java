package com.integrity.organization.web.dto;

import com.integrity.organization.domain.OrganizationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change the lifecycle status of an organization.
 *
 * @param status target status
 */
public record ChangeOrganizationStatusRequest(@NotNull OrganizationStatus status) {}
