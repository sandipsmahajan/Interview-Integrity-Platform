package com.integrity.interview.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an interviewer panel membership.
 *
 * @param interviewId interview identifier
 * @param interviewerId interviewer identifier
 * @param role role on the panel
 * @param addedBy user that added the interviewer
 * @param addedAt instant the interviewer joined the panel
 */
public record InterviewPanelResponse(
    UUID interviewId, UUID interviewerId, String role, UUID addedBy, Instant addedAt) {}
