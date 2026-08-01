package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to rename a department.
 *
 * @param name department name
 */
public record UpdateDepartmentRequest(@NotBlank @Size(max = 120) String name) {}
