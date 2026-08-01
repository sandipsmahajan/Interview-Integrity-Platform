package com.interviewintegrity.policy.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.repository.PolicyRuleRepository;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the evaluable rules of a policy. */
public class PolicyRuleService {

  private final PolicyRuleRepository ruleRepository;
  private final PolicyService policyService;

  /** Wires the service with its collaborators. */
  public PolicyRuleService(PolicyRuleRepository ruleRepository, PolicyService policyService) {
    this.ruleRepository = ruleRepository;
    this.policyService = policyService;
  }

  /** Adds a rule to a policy owned by the organization. */
  public Mono<PolicyRule> create(
      UUID organizationId,
      UUID policyId,
      String ruleCode,
      String description,
      String condition,
      ViolationSeverity severity,
      Integer weight,
      Integer orderIndex,
      UUID createdBy) {
    return policyService
        .get(organizationId, policyId)
        .flatMap(
            policy ->
                ruleRepository.insert(
                    new PolicyRule(
                        organizationId,
                        policyId,
                        ruleCode,
                        description,
                        condition,
                        severity,
                        weight,
                        orderIndex,
                        createdBy)));
  }

  /** Returns a single rule, validating tenant ownership. */
  public Mono<PolicyRule> get(UUID organizationId, UUID id) {
    return ruleRepository
        .findById(id, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Rule not found")))
        .flatMap(rule -> assertOrganization(rule, organizationId));
  }

  /** Lists the rules of a policy owned by the organization. */
  public Flux<PolicyRule> list(UUID organizationId, UUID policyId) {
    return policyService
        .get(organizationId, policyId)
        .flatMapMany(policy -> ruleRepository.listByPolicy(policyId));
  }

  /** Updates the editable attributes of a rule. */
  public Mono<PolicyRule> update(
      UUID organizationId,
      UUID id,
      String ruleCode,
      String description,
      String condition,
      ViolationSeverity severity,
      Integer weight,
      Integer orderIndex,
      Boolean enabled,
      UUID updatedBy) {
    return get(organizationId, id)
        .flatMap(
            rule ->
                ruleRepository.update(
                    id,
                    organizationId,
                    ruleCode,
                    description,
                    condition,
                    severity,
                    weight,
                    orderIndex,
                    enabled,
                    updatedBy));
  }

  /** Soft deletes a rule. */
  public Mono<Void> delete(UUID organizationId, UUID id, UUID deletedBy) {
    return get(organizationId, id)
        .flatMap(rule -> ruleRepository.softDelete(id, organizationId, deletedBy));
  }

  private Mono<PolicyRule> assertOrganization(PolicyRule rule, UUID organizationId) {
    return Mono.justOrEmpty(rule)
        .flatMap(
            r -> {
              if (!organizationId.equals(r.getOrganizationId())) {
                return Mono.error(new NotFoundException("Rule not found"));
              }
              return Mono.just(r);
            });
  }
}
