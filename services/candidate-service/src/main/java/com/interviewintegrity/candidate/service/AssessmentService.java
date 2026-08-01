package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.Assessment;
import com.interviewintegrity.candidate.domain.AssessmentStatus;
import com.interviewintegrity.candidate.repository.AssessmentRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages assessments assigned to candidates. */
public class AssessmentService {

  private final AssessmentRepository assessmentRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repository and the candidate service. */
  public AssessmentService(
      AssessmentRepository assessmentRepository, CandidateService candidateService) {
    this.assessmentRepository = assessmentRepository;
    this.candidateService = candidateService;
  }

  /** Assigns an assessment to a candidate. */
  @Transactional
  public Mono<Assessment> create(
      UUID organizationId,
      UUID candidateId,
      String assessmentType,
      Instant expiresAt,
      UUID assignedBy) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .then(
            assessmentRepository.save(
                new Assessment(
                    organizationId, candidateId, assessmentType.trim(), assignedBy, expiresAt)));
  }

  /** Lists the assessments of a candidate. */
  @Transactional(readOnly = true)
  public Flux<Assessment> list(UUID organizationId, UUID candidateId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .thenMany(assessmentRepository.listByOrganizationAndCandidate(organizationId, candidateId));
  }

  /** Starts an assigned assessment. */
  @Transactional
  public Mono<Assessment> start(UUID assessmentId, UUID organizationId) {
    return requireAssessment(assessmentId, organizationId)
        .flatMap(
            assessment -> {
              if (assessment.getStatus() != AssessmentStatus.ASSIGNED) {
                return Mono.error(
                    new ConflictException(
                        "Assessment cannot be started in status " + assessment.getStatus()));
              }
              assessment.start();
              return assessmentRepository.save(assessment);
            });
  }

  /** Completes an assessment with an optional score. */
  @Transactional
  public Mono<Assessment> complete(UUID assessmentId, UUID organizationId, BigDecimal score) {
    return requireAssessment(assessmentId, organizationId)
        .flatMap(
            assessment -> {
              if (assessment.getStatus() == AssessmentStatus.COMPLETED
                  || assessment.getStatus() == AssessmentStatus.EXPIRED) {
                return Mono.error(
                    new ConflictException(
                        "Assessment already "
                            + assessment.getStatus().name().toLowerCase(Locale.ROOT)));
              }
              assessment.complete(score);
              return assessmentRepository.save(assessment);
            });
  }

  /** Expires an assessment that has not been completed. */
  @Transactional
  public Mono<Assessment> expire(UUID assessmentId, UUID organizationId) {
    return requireAssessment(assessmentId, organizationId)
        .flatMap(
            assessment -> {
              if (assessment.getStatus() == AssessmentStatus.COMPLETED) {
                return Mono.error(new ConflictException("Assessment already completed"));
              }
              assessment.expire();
              return assessmentRepository.save(assessment);
            });
  }

  private Mono<Assessment> requireAssessment(UUID assessmentId, UUID organizationId) {
    return assessmentRepository
        .findById(assessmentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Assessment not found")))
        .flatMap(
            assessment -> {
              if (!organizationId.equals(assessment.getOrganizationId())) {
                return Mono.error(new NotFoundException("Assessment not found"));
              }
              return Mono.just(assessment);
            });
  }
}
