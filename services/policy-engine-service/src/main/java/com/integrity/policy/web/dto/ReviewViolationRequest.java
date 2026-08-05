package com.integrity.policy.web.dto;

import com.integrity.policy.domain.ReviewAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to review a violation.
 *
 * @param action decision taken by the reviewer
 * @param comment optional note recorded with the decision
 * @param escalatedTo required reviewer for the {@code ESCALATE} action
 */
public record ReviewViolationRequest(
    @NotNull ReviewAction action, @Size(max = 2000) String comment, UUID escalatedTo) {}
