package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to add a user to a team.
 *
 * @param userId identifier of the user to add
 */
public record AddTeamMemberRequest(@NotNull UUID userId) {}
