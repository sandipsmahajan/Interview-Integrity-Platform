package com.integrity.policy.web.dto;

import com.integrity.policy.domain.ViolationSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a policy rule.
 *
 * @param id identifier
 * @param policyId owning policy
 * @param ruleCode rule code
 * @param description description
 * @param condition JSONB predicate
 * @param severity violation severity
 * @param weight relative weight
 * @param orderIndex evaluation order
 * @param enabled whether the rule participates in evaluation
 * @param createdAt creation instant
 * @param updatedAt last update instant
 * @param version optimistic lock version
 */
public record RuleResponse(
    UUID id,
    UUID policyId,
    String ruleCode,
    String description,
    String condition,
    ViolationSeverity severity,
    int weight,
    int orderIndex,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt,
    long version) {}
