package com.integrity.policy.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * One event offered to the policy evaluator.
 *
 * @param eventType telemetry event type code
 * @param data event payload fields used by rule conditions
 */
public record EvaluateEventRequest(@NotBlank String eventType, Map<String, Object> data) {}
