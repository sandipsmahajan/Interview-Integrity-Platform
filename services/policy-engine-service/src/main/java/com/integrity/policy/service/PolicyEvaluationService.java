package com.integrity.policy.service;

import com.integrity.policy.domain.PolicyRule;
import com.integrity.policy.domain.Violation;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic rule evaluator for the policy engine.
 *
 * <p>A rule condition is a JSON predicate like {@code {"eventType":"HEARTBEAT","op":"gt",
 * "field":"secondsSincePreviousHeartbeat","value":10}}; an event matches when its type equals the
 * declared {@code eventType} and the {@code op} comparison against {@code field} holds.
 */
public class PolicyEvaluationService {

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationService.class);

  private static final String OPERATOR_EQ = "eq";
  private static final String OPERATOR_NEQ = "neq";

  private final PolicyRuleService ruleService;
  private final ViolationService violationService;
  private final ObjectMapper objectMapper;

  /** Wires the evaluator with its collaborators. */
  public PolicyEvaluationService(PolicyRuleService ruleService, ViolationService violationService) {
    this.ruleService = ruleService;
    this.violationService = violationService;
    this.objectMapper = new ObjectMapper();
  }

  /** Returns the enabled rules of a policy that match the given event. */
  public Flux<PolicyRule> evaluate(UUID organizationId, UUID policyId, EvaluationEvent event) {
    return ruleService
        .list(organizationId, policyId)
        .filter(rule -> rule.isEnabled() && matches(rule.getCondition(), event));
  }

  /** Evaluates a policy and records a violation for every matching rule. */
  public Flux<Violation> violate(
      UUID organizationId, UUID policyId, EvaluationEvent event, UUID detectedBy) {
    return evaluate(organizationId, policyId, event)
        .flatMap(
            rule ->
                violationService.record(
                    organizationId,
                    null,
                    null,
                    policyId,
                    rule.getRuleCode(),
                    rule.getSeverity(),
                    "Policy " + policyId + " rule " + rule.getRuleCode() + " matched",
                    serialize(event),
                    Instant.now(),
                    detectedBy == null ? "policy-engine-service" : detectedBy.toString()));
  }

  boolean matches(String conditionJson, EvaluationEvent event) {
    if (conditionJson == null || conditionJson.isBlank()) {
      return false;
    }
    try {
      JsonNode condition = objectMapper.readTree(conditionJson);
      JsonNode eventType = condition.get("eventType");
      if (eventType == null
          || eventType.isNull()
          || !eventType.asString().equals(event.eventType())) {
        return false;
      }
      JsonNode op = condition.get("op");
      JsonNode field = condition.get("field");
      JsonNode expected = condition.get("value");
      if (op == null || field == null || expected == null) {
        return false;
      }
      Object actual = event.data() == null ? null : event.data().get(field.asString());
      return compare(op.asString(), actual, expected);
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn("Skipping unparsable rule condition {}: {}", conditionJson, e.getMessage());
      }
      return false;
    }
  }

  private boolean compare(String op, Object actual, JsonNode expected) {
    if (OPERATOR_EQ.equals(op)) {
      return valuesEqual(actual, expected);
    }
    if (OPERATOR_NEQ.equals(op)) {
      return !valuesEqual(actual, expected);
    }
    Double left = asDouble(actual);
    Double right;
    if (expected.isNumber()) {
      right = expected.asDouble();
    } else {
      right = asDouble(expected.asString());
    }
    if (left == null || right == null) {
      return false;
    }
    return switch (op) {
      case "gt" -> left > right;
      case "gte" -> left >= right;
      case "lt" -> left < right;
      case "lte" -> left <= right;
      default -> false;
    };
  }

  private boolean valuesEqual(Object actual, JsonNode expected) {
    if (actual == null) {
      return expected.isNull();
    }
    if (expected.isNumber() && actual instanceof Number) {
      return expected.asDouble() == ((Number) actual).doubleValue();
    }
    if (expected.isBoolean() && actual instanceof Boolean) {
      return expected.asBoolean() == (Boolean) actual;
    }
    return expected.asString().equals(String.valueOf(actual));
  }

  private Double asDouble(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    if (value instanceof String text) {
      try {
        return Double.valueOf(text);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  private String serialize(EvaluationEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (Exception e) {
      return "{}";
    }
  }
}
