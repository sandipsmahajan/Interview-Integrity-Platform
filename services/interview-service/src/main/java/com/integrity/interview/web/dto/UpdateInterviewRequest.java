package com.integrity.interview.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update an interview.
 *
 * @param title interview title
 * @param meetingUrl optional meeting link
 * @param metadata free form JSON metadata
 */
public record UpdateInterviewRequest(
    @NotBlank @Size(max = 250) String title,
    @Size(max = 500) String meetingUrl,
    @Size(max = 16000) String metadata) {}
