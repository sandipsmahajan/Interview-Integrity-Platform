package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to create a department.
 *
 * @param parentId optional parent department
 * @param name department name
 */
public record CreateDepartmentRequest(UUID parentId, @NotBlank @Size(max = 120) String name) {}
