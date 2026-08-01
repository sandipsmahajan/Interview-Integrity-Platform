package com.interviewintegrity.policy.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.exception.ValidationFailedException;
import com.interviewintegrity.policy.domain.ReviewAction;
import com.interviewintegrity.policy.domain.Violation;
import com.interviewintegrity.policy.domain.ViolationEscalation;
import com.interviewintegrity.policy.domain.ViolationReview;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import com.interviewintegrity.policy.domain.ViolationStatus;
import com.interviewintegrity.policy.repository.ViolationEscalationRepository;
import com.interviewintegrity.policy.repository.ViolationRepository;
import com.interviewintegrity.policy.repository.ViolationReviewRepository;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Triage service over detected violations. */
public class ViolationService {

  private final ViolationRepository violationRepository;
  private final ViolationReviewRepository reviewRepository;
  private final ViolationEscalationRepository escalationRepository;

  /** Wires the service with its repositories. */
  public ViolationService(
      ViolationRepository violationRepository,
      ViolationReviewRepository reviewRepository,
      ViolationEscalationRepository escalationRepository) {
    this.violationRepository = violationRepository;
    this.reviewRepository = reviewRepository;
    this.escalationRepository = escalationRepository;
  }

  /** Persists a detected violation. */
  public Mono<Violation> record(
      UUID organizationId,
      UUID sessionId,
      UUID interviewId,
      UUID policyId,
      String ruleCode,
      ViolationSeverity severity,
      String message,
      String evidence,
      Instant occurredAt,
      String detectedBy) {
    return violationRepository.insert(
        new Violation(
            organizationId,
            sessionId,
            interviewId,
            policyId,
            ruleCode,
            severity,
            message,
            evidence,
            occurredAt,
            detectedBy));
  }

  /** Lists the violations of an organization with optional filters. */
  public Flux<Violation> list(
      UUID organizationId, ViolationStatus status, ViolationSeverity severity) {
    return violationRepository.listByOrganization(organizationId, status, severity);
  }

  /** Returns a single violation, validating tenant ownership. */
  public Mono<Violation> get(UUID organizationId, UUID id) {
    return violationRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Violation not found")))
        .flatMap(violation -> assertOrganization(violation, organizationId));
  }

  /** Checks whether a violation with the same fingerprint already exists. */
  public Mono<Boolean> exists(UUID sessionId, String ruleCode, Instant occurredAt) {
    return violationRepository.exists(sessionId, ruleCode, occurredAt);
  }

  /**
   * Records a human review decision and applies the resulting triage transition.
   *
   * <p>{@code CONFIRM} resolves the violation, {@code DISMISS} dismisses it and {@code ESCALATE}
   * moves it to the escalated state while opening an escalation to the given reviewer.
   */
  public Mono<Violation> review(
      UUID organizationId,
      UUID violationId,
      UUID reviewerId,
      ReviewAction action,
      String comment,
      UUID escalatedTo) {
    return get(organizationId, violationId)
        .flatMap(
            violation ->
                reviewRepository
                    .insert(
                        new ViolationReview(
                            organizationId, violationId, reviewerId, action, comment))
                    .flatMap(
                        ignored -> {
                          ViolationStatus target = targetStatus(action);
                          if (action == ReviewAction.ESCALATE && escalatedTo == null) {
                            return Mono.error(
                                new ValidationFailedException(
                                    "escalatedTo must be provided for ESCALATE"));
                          }
                          Mono<Violation> transition =
                              violationRepository.updateStatus(violationId, organizationId, target);
                          if (action != ReviewAction.ESCALATE) {
                            return transition;
                          }
                          return escalationRepository
                              .insert(
                                  new ViolationEscalation(
                                      organizationId,
                                      violationId,
                                      escalatedTo,
                                      comment,
                                      reviewerId))
                              .then(transition);
                        }));
  }

  private static ViolationStatus targetStatus(ReviewAction action) {
    return switch (action) {
      case CONFIRM -> ViolationStatus.RESOLVED;
      case DISMISS -> ViolationStatus.DISMISSED;
      case ESCALATE -> ViolationStatus.ESCALATED;
    };
  }

  private Mono<Violation> assertOrganization(Violation violation, UUID organizationId) {
    return Mono.justOrEmpty(violation)
        .flatMap(
            v -> {
              if (!organizationId.equals(v.getOrganizationId())) {
                return Mono.error(new NotFoundException("Violation not found"));
              }
              return Mono.just(v);
            });
  }
}
