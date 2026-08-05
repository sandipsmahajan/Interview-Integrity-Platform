package com.integrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Membership of a user in a team.
 *
 * @param teamId team identifier
 * @param userId member user identifier
 * @param addedBy user that added the member
 * @param addedAt instant the member was added
 */
public record TeamMemberResponse(UUID teamId, UUID userId, UUID addedBy, Instant addedAt) {}
