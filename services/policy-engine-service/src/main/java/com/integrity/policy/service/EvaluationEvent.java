package com.integrity.policy.service;

import java.util.Map;

/**
 * One event offered to the policy evaluator.
 *
 * @param eventType telemetry event type code
 * @param data event payload fields used by rule conditions
 */
public record EvaluationEvent(String eventType, Map<String, Object> data) {}
