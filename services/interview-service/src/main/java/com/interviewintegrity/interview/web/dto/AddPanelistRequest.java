package com.interviewintegrity.interview.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to add an interviewer to a panel.
 *
 * @param interviewerId interviewer to add
 * @param role role on the panel
 */
public record AddPanelistRequest(
    @NotNull UUID interviewerId, @NotBlank @Size(max = 60) String role) {}
