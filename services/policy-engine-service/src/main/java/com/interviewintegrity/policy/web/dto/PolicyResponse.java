package com.interviewintegrity.policy.web.dto;

import com.interviewintegrity.policy.domain.PolicyStatus;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a policy.
 *
 * @param id identifier
 * @param organizationId owning tenant
 * @param code stable policy code
 * @param name display name
 * @param description description
 * @param status lifecycle state
 * @param defaultSeverity default severity for unmatched rules
 * @param priority evaluation priority
 * @param enabled whether the policy participates in evaluation
 * @param createdAt creation instant
 * @param updatedAt last update instant
 * @param version optimistic lock version
 */
public record PolicyResponse(
    UUID id,
    UUID organizationId,
    String code,
    String name,
    String description,
    PolicyStatus status,
    ViolationSeverity defaultSeverity,
    int priority,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt,
    long version) {}
