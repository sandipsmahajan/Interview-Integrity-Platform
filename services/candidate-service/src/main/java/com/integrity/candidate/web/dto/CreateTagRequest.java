package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a tag.
 *
 * @param code stable machine readable code
 * @param name display name
 */
public record CreateTagRequest(
    @NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 150) String name) {}
