package com.interviewintegrity.candidate.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a candidate note.
 *
 * @param id note identifier
 * @param candidateId subject candidate
 * @param authorId authoring user
 * @param body note text
 * @param pinned whether the note is pinned
 * @param createdAt instant the note was created
 * @param updatedAt instant the note was last modified
 */
public record CandidateNoteResponse(
    UUID id,
    UUID candidateId,
    UUID authorId,
    String body,
    boolean pinned,
    Instant createdAt,
    Instant updatedAt) {}
