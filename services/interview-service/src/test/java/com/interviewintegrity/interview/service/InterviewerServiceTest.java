package com.interviewintegrity.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.interview.domain.Interviewer;
import com.interviewintegrity.interview.repository.InterviewerRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the interviewer profile service. */
@ExtendWith(MockitoExtension.class)
class InterviewerServiceTest {

  @Mock private InterviewerRepository interviewerRepository;

  private InterviewerService interviewerService;

  @BeforeEach
  void setUp() {
    interviewerService = new InterviewerService(interviewerRepository);
  }

  @Test
  void createSavesInterviewer() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();

    when(interviewerRepository.findLiveByOrganizationAndUser(organizationId, userId))
        .thenReturn(Mono.empty());
    when(interviewerRepository.save(any(Interviewer.class)))
        .thenAnswer(
            invocation -> {
              Interviewer interviewer = invocation.getArgument(0);
              interviewer.setId(UUID.randomUUID());
              return Mono.just(interviewer);
            });

    StepVerifier.create(
            interviewerService.create(
                organizationId, userId, "Ada Lovelace", "ada@example.com", byUser))
        .assertNext(
            interviewer -> {
              assertThat(interviewer.getFullName()).isEqualTo("Ada Lovelace");
              assertThat(interviewer.getEmail()).isEqualTo("ada@example.com");
            })
        .verifyComplete();
  }

  @Test
  void createRejectsDuplicateUser() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(interviewerRepository.findLiveByOrganizationAndUser(organizationId, userId))
        .thenReturn(
            Mono.just(
                new Interviewer(
                    organizationId, userId, "Ada Lovelace", "ada@example.com", userId)));

    StepVerifier.create(
            interviewerService.create(
                organizationId, userId, "Ada Lovelace", "ada@example.com", userId))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void updateChangesProfile() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewerId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Interviewer interviewer =
        new Interviewer(organizationId, userId, "Ada Lovelace", "ada@example.com", byUser);
    interviewer.setId(interviewerId);

    when(interviewerRepository.findLiveById(interviewerId)).thenReturn(Mono.just(interviewer));
    when(interviewerRepository.save(any(Interviewer.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            interviewerService.update(
                interviewerId, organizationId, "Ada L.", "ada2@example.com", byUser))
        .assertNext(
            result -> {
              assertThat(result.getFullName()).isEqualTo("Ada L.");
              assertThat(result.getEmail()).isEqualTo("ada2@example.com");
            })
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID interviewerId = UUID.randomUUID();
    when(interviewerRepository.findLiveById(interviewerId)).thenReturn(Mono.empty());

    StepVerifier.create(interviewerService.get(interviewerId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }
}
