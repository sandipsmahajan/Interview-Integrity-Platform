package com.interviewintegrity.candidate.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to apply a tag to a candidate.
 *
 * @param tagId tag to apply
 */
public record AttachTagRequest(@NotNull UUID tagId) {}
