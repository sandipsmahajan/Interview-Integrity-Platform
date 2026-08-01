package com.interviewintegrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.candidate.domain.Assessment;
import com.interviewintegrity.candidate.domain.AssessmentStatus;
import com.interviewintegrity.candidate.domain.Candidate;
import com.interviewintegrity.candidate.repository.AssessmentRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the assessment service. */
class AssessmentServiceTest {

  private final AssessmentRepository assessmentRepository =
      Mockito.mock(AssessmentRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private AssessmentService assessmentService;

  @BeforeEach
  void setUp() {
    assessmentService = new AssessmentService(assessmentRepository, candidateService);
  }

  @Test
  void createAssignsAssessmentToCandidate() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID assignedBy = UUID.randomUUID();
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(assessmentRepository.save(any(Assessment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            assessmentService.create(organizationId, candidateId, "CODING", null, assignedBy))
        .assertNext(
            assessment -> {
              assertThat(assessment.getAssessmentType()).isEqualTo("CODING");
              assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.ASSIGNED);
              assertThat(assessment.getAssignedBy()).isEqualTo(assignedBy);
            })
        .verifyComplete();
  }

  @Test
  void startMovesAssignedAssessmentToInProgress() {
    UUID assessmentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Assessment assessment = assessment(assessmentId, organizationId);
    when(assessmentRepository.findById(assessmentId)).thenReturn(Mono.just(assessment));
    when(assessmentRepository.save(any(Assessment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(assessmentService.start(assessmentId, organizationId))
        .assertNext(
            started -> assertThat(started.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS))
        .verifyComplete();
  }

  @Test
  void startRejectsAssessmentAlreadyInProgress() {
    UUID assessmentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Assessment assessment = assessment(assessmentId, organizationId);
    assessment.start();
    when(assessmentRepository.findById(assessmentId)).thenReturn(Mono.just(assessment));

    StepVerifier.create(assessmentService.start(assessmentId, organizationId))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void completeRecordsScoreAndStatus() {
    UUID assessmentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Assessment assessment = assessment(assessmentId, organizationId);
    when(assessmentRepository.findById(assessmentId)).thenReturn(Mono.just(assessment));
    when(assessmentRepository.save(any(Assessment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            assessmentService.complete(assessmentId, organizationId, new BigDecimal("87.5")))
        .assertNext(
            completed -> {
              assertThat(completed.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
              assertThat(completed.getScore()).isEqualByComparingTo(new BigDecimal("87.5"));
            })
        .verifyComplete();
  }

  @Test
  void expireReturnsNotFoundForUnknownAssessment() {
    UUID assessmentId = UUID.randomUUID();
    when(assessmentRepository.findById(assessmentId)).thenReturn(Mono.empty());

    StepVerifier.create(assessmentService.expire(assessmentId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  private static Assessment assessment(UUID assessmentId, UUID organizationId) {
    Assessment assessment =
        new Assessment(organizationId, UUID.randomUUID(), "CODING", UUID.randomUUID(), null);
    assessment.setId(assessmentId);
    return assessment;
  }

  private static Candidate candidate(UUID candidateId, UUID organizationId) {
    Candidate candidate =
        new Candidate(organizationId, null, "a@b.com", "Jane", null, null, UUID.randomUUID());
    candidate.setId(candidateId);
    return candidate;
  }
}
