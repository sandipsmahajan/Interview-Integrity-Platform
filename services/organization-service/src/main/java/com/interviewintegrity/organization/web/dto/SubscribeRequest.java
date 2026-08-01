package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to subscribe an organization to a plan.
 *
 * @param planCode code of the plan to subscribe to
 * @param provider optional billing provider reference
 */
public record SubscribeRequest(
    @NotBlank @Size(max = 60) String planCode, @Size(max = 120) String provider) {}
