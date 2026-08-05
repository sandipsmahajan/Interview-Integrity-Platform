package com.integrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create a team.
 *
 * @param departmentId owning department
 * @param name team name
 */
public record CreateTeamRequest(UUID departmentId, @NotBlank @Size(max = 120) String name) {}
