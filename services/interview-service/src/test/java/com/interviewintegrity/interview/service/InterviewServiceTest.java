package com.interviewintegrity.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.interview.domain.Interview;
import com.interviewintegrity.interview.domain.InterviewMode;
import com.interviewintegrity.interview.domain.InterviewStatus;
import com.interviewintegrity.interview.repository.InterviewRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the interview lifecycle service. */
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

  @Mock private InterviewRepository interviewRepository;
  @Mock private InterviewEventPublisher eventPublisher;

  private InterviewService interviewService;

  @BeforeEach
  void setUp() {
    interviewService = new InterviewService(interviewRepository, eventPublisher);
  }

  @Test
  void createSavesInterviewAndPublishesCreatedEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID recruiterId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Instant startsAt = Instant.now();
    Instant endsAt = startsAt.plusSeconds(3600);

    when(interviewRepository.save(any(Interview.class)))
        .thenAnswer(
            invocation -> {
              Interview interview = invocation.getArgument(0);
              interview.setId(UUID.randomUUID());
              return Mono.just(interview);
            });
    when(eventPublisher.publishCreated(any(Interview.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            interviewService.create(
                organizationId,
                candidateId,
                recruiterId,
                1,
                "Phone screen",
                InterviewMode.ONLINE,
                null,
                startsAt,
                endsAt,
                "UTC",
                null,
                byUser))
        .assertNext(
            interview -> {
              assertThat(interview.getOrganizationId()).isEqualTo(organizationId);
              assertThat(interview.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
              assertThat(interview.getMode()).isEqualTo(InterviewMode.ONLINE);
              assertThat(interview.getMetadata()).isEqualTo("{}");
              assertThat(interview.getTimezone()).isEqualTo("UTC");
            })
        .verifyComplete();

    verify(eventPublisher).publishCreated(any(Interview.class));
  }

  @Test
  void scheduleUpdatesInterviewAndPublishesScheduledEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID recruiterId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Instant startsAt = Instant.now();
    Interview interview =
        interview(organizationId, interviewId, candidateId, recruiterId, startsAt, byUser);

    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));
    when(interviewRepository.save(any(Interview.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(eventPublisher.publishScheduled(any(Interview.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            interviewService.schedule(
                interviewId,
                organizationId,
                startsAt.plusSeconds(3600),
                startsAt.plusSeconds(5400),
                "Europe/Berlin",
                "https://meet.example.com",
                byUser))
        .assertNext(
            result -> {
              assertThat(result.getTimezone()).isEqualTo("Europe/Berlin");
              assertThat(result.getMeetingUrl()).isEqualTo("https://meet.example.com");
              assertThat(result.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
            })
        .verifyComplete();

    verify(eventPublisher).publishScheduled(any(Interview.class));
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID interviewId = UUID.randomUUID();
    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.empty());

    StepVerifier.create(interviewService.get(interviewId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID recruiterId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Interview interview =
        interview(organizationId, interviewId, candidateId, recruiterId, Instant.now(), byUser);

    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));

    StepVerifier.create(interviewService.get(interviewId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void cancelMarksInterviewAsCancelled() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID recruiterId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Interview interview =
        interview(organizationId, interviewId, candidateId, recruiterId, Instant.now(), byUser);

    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));
    when(interviewRepository.save(any(Interview.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(interviewService.cancel(interviewId, organizationId, byUser))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(InterviewStatus.CANCELLED))
        .verifyComplete();
  }

  private static Interview interview(
      UUID organizationId,
      UUID interviewId,
      UUID candidateId,
      UUID recruiterId,
      Instant startsAt,
      UUID byUser) {
    Interview interview =
        new Interview(
            organizationId,
            candidateId,
            recruiterId,
            1,
            "Phone screen",
            InterviewMode.ONLINE,
            null,
            startsAt,
            startsAt.plusSeconds(1800),
            "UTC",
            null,
            byUser);
    interview.setId(interviewId);
    return interview;
  }
}
