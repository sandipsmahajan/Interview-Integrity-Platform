package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update a note.
 *
 * @param body note text
 */
public record UpdateNoteRequest(@NotBlank @Size(max = 4000) String body) {}
