package com.integrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateStatus;
import com.integrity.candidate.repository.CandidateRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the candidate lifecycle service. */
class CandidateServiceTest {

  private static final String FULL_NAME = "Jane Doe";
  private static final String FIRST_NAME = "Jane";
  private static final String EMAIL = "a@b.com";

  private final CandidateRepository candidateRepository = Mockito.mock(CandidateRepository.class);
  private final CandidateEventPublisher eventPublisher =
      Mockito.mock(CandidateEventPublisher.class);

  private CandidateService candidateService;

  @BeforeEach
  void setUp() {
    candidateService = new CandidateService(candidateRepository, eventPublisher);
  }

  @Test
  void createNormalizesEmailAndPublishesEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(candidateRepository.existsLiveByOrganizationAndEmail(organizationId, "jane@example.com"))
        .thenReturn(Mono.just(false));
    when(candidateRepository.save(any(Candidate.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(eventPublisher.publishCandidateRegistered(any(Candidate.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            candidateService.create(
                organizationId, userId, "Jane@Example.com", FULL_NAME, null, null, userId))
        .assertNext(
            candidate -> {
              assertThat(candidate.getEmail()).isEqualTo("jane@example.com");
              assertThat(candidate.getFullName()).isEqualTo(FULL_NAME);
              assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.NEW);
            })
        .verifyComplete();
    verify(eventPublisher).publishCandidateRegistered(any(Candidate.class));
  }

  @Test
  void createRejectsDuplicateEmail() {
    when(candidateRepository.existsLiveByOrganizationAndEmail(any(), any()))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            candidateService.create(
                UUID.randomUUID(), UUID.randomUUID(), EMAIL, FIRST_NAME, null, null, null))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void getByIdReturnsNotFoundForUnknownCandidate() {
    UUID id = UUID.randomUUID();
    when(candidateRepository.findLiveById(id)).thenReturn(Mono.empty());

    StepVerifier.create(candidateService.getById(id, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getByIdRejectsCrossTenantCandidate() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Candidate candidate =
        new Candidate(organizationId, null, EMAIL, FIRST_NAME, null, null, UUID.randomUUID());
    candidate.setId(id);
    when(candidateRepository.findLiveById(id)).thenReturn(Mono.just(candidate));

    StepVerifier.create(candidateService.getById(id, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateSavesMutatedCandidate() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Candidate candidate =
        new Candidate(organizationId, null, EMAIL, FIRST_NAME, null, null, byUser);
    candidate.setId(id);
    when(candidateRepository.findLiveById(id)).thenReturn(Mono.just(candidate));
    when(candidateRepository.save(any(Candidate.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            candidateService.update(id, organizationId, FULL_NAME, "123", "referral", byUser))
        .assertNext(
            updated -> {
              assertThat(updated.getFullName()).isEqualTo(FULL_NAME);
              assertThat(updated.getPhone()).isEqualTo("123");
              assertThat(updated.getSource()).isEqualTo("referral");
            })
        .verifyComplete();
  }

  @Test
  void changeStatusMovesCandidateThroughLifecycle() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Candidate candidate =
        new Candidate(organizationId, null, EMAIL, FIRST_NAME, null, null, UUID.randomUUID());
    candidate.setId(id);
    when(candidateRepository.findLiveById(id)).thenReturn(Mono.just(candidate));
    when(candidateRepository.save(any(Candidate.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            candidateService.changeStatus(
                id, organizationId, CandidateStatus.OFFERED, UUID.randomUUID()))
        .assertNext(updated -> assertThat(updated.getStatus()).isEqualTo(CandidateStatus.OFFERED))
        .verifyComplete();
  }

  @Test
  void deleteSoftDeletesCandidate() {
    UUID id = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    Candidate candidate =
        new Candidate(organizationId, null, EMAIL, FIRST_NAME, null, null, UUID.randomUUID());
    candidate.setId(id);
    when(candidateRepository.findLiveById(id)).thenReturn(Mono.just(candidate));
    when(candidateRepository.save(any(Candidate.class))).thenReturn(Mono.just(candidate));

    StepVerifier.create(candidateService.delete(id, organizationId, UUID.randomUUID()))
        .verifyComplete();
    assertThat(candidate.getDeletedAt()).isNotNull();
  }
}
