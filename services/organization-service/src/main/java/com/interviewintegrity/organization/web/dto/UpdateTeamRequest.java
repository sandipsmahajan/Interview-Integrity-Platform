package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to rename a team.
 *
 * @param name team name
 */
public record UpdateTeamRequest(@NotBlank @Size(max = 120) String name) {}
