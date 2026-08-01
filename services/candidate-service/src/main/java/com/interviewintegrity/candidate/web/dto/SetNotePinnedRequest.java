package com.interviewintegrity.candidate.web.dto;

/**
 * Request to pin or unpin a candidate note.
 *
 * @param pinned whether the note should be pinned
 */
public record SetNotePinnedRequest(boolean pinned) {}
