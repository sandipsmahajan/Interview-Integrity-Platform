package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to change the role of an assignment.
 *
 * @param role role within the assignment
 */
public record ChangeAssignmentRoleRequest(@NotBlank @Size(max = 80) String role) {}
