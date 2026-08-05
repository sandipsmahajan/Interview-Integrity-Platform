package com.integrity.recruiter.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a recruiter note.
 *
 * @param id note identifier
 * @param recruiterId author
 * @param candidateId subject candidate
 * @param body note text
 * @param createdAt instant the note was created
 * @param updatedAt instant the note was last modified
 */
public record RecruiterNoteResponse(
    UUID id,
    UUID recruiterId,
    UUID candidateId,
    String body,
    Instant createdAt,
    Instant updatedAt) {}
