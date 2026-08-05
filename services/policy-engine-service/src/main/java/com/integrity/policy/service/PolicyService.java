package com.integrity.policy.service;

import com.integrity.exception.NotFoundException;
import com.integrity.policy.domain.Policy;
import com.integrity.policy.domain.PolicyStatus;
import com.integrity.policy.domain.ViolationSeverity;
import com.integrity.policy.repository.PolicyRepository;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the policy catalog and its lifecycle. */
public class PolicyService {

  private final PolicyRepository policyRepository;
  private final PolicyPublishingService publishingService;

  /** Wires the service with its collaborators. */
  public PolicyService(
      PolicyRepository policyRepository, PolicyPublishingService publishingService) {
    this.policyRepository = policyRepository;
    this.publishingService = publishingService;
  }

  /** Creates a new draft policy. */
  public Mono<Policy> create(
      UUID organizationId,
      String code,
      String name,
      String description,
      ViolationSeverity defaultSeverity,
      Integer priority,
      UUID createdBy) {
    return policyRepository.insert(
        new Policy(organizationId, code, name, description, defaultSeverity, priority, createdBy));
  }

  /** Returns a single policy, validating tenant ownership. */
  public Mono<Policy> get(UUID organizationId, UUID id) {
    return policyRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Policy not found")))
        .flatMap(policy -> assertOrganization(policy, organizationId));
  }

  /** Lists the live policies of an organization. */
  public Flux<Policy> list(UUID organizationId) {
    return policyRepository.listByOrganization(organizationId);
  }

  /** Updates the editable attributes of a policy. */
  public Mono<Policy> update(
      UUID organizationId,
      UUID id,
      String name,
      String description,
      ViolationSeverity defaultSeverity,
      Integer priority,
      Boolean enabled,
      UUID updatedBy) {
    return get(organizationId, id)
        .flatMap(
            policy ->
                policyRepository.update(
                    id,
                    organizationId,
                    name,
                    description,
                    policy.getStatus(),
                    defaultSeverity,
                    priority,
                    enabled,
                    updatedBy));
  }

  /** Transitions a policy lifecycle state, snapshotting a version on activation. */
  public Mono<Policy> changeStatus(
      UUID organizationId, UUID id, PolicyStatus status, UUID updatedBy) {
    return get(organizationId, id)
        .flatMap(
            policy -> {
              PolicyStatus target = status == null ? policy.getStatus() : status;
              return policyRepository
                  .changeStatus(id, organizationId, target, updatedBy)
                  .flatMap(
                      updated ->
                          target == PolicyStatus.ACTIVE
                              ? publishingService
                                  .publishVersion(updated, updatedBy)
                                  .thenReturn(updated)
                              : Mono.just(updated));
            });
  }

  /** Soft deletes a policy. */
  public Mono<Void> delete(UUID organizationId, UUID id, UUID deletedBy) {
    return get(organizationId, id)
        .flatMap(policy -> policyRepository.softDelete(id, organizationId, deletedBy));
  }

  private Mono<Policy> assertOrganization(Policy policy, UUID organizationId) {
    return Mono.justOrEmpty(policy)
        .flatMap(
            p -> {
              if (!organizationId.equals(p.getOrganizationId())) {
                return Mono.error(new NotFoundException("Policy not found"));
              }
              return Mono.just(p);
            });
  }
}
