package com.interviewintegrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile of a team.
 *
 * @param id team identifier
 * @param organizationId owning tenant
 * @param departmentId owning department
 * @param name team name
 * @param createdAt instant the team was created
 */
public record TeamResponse(
    UUID id, UUID organizationId, UUID departmentId, String name, Instant createdAt) {}
