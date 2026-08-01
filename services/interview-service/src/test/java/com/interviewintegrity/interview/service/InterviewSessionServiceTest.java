package com.interviewintegrity.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.interview.domain.Interview;
import com.interviewintegrity.interview.domain.InterviewMode;
import com.interviewintegrity.interview.domain.InterviewSession;
import com.interviewintegrity.interview.domain.SessionStatus;
import com.interviewintegrity.interview.repository.InterviewRepository;
import com.interviewintegrity.interview.repository.InterviewSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the interview session lifecycle service. */
@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceTest {

  @Mock private InterviewSessionRepository sessionRepository;
  @Mock private InterviewRepository interviewRepository;
  @Mock private InterviewEventPublisher eventPublisher;

  private InterviewSessionService sessionService;

  @BeforeEach
  void setUp() {
    sessionService =
        new InterviewSessionService(sessionRepository, interviewRepository, eventPublisher);
  }

  @Test
  void startActivatesSessionAndPublishesStartedEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Interview interview = interview(organizationId, interviewId, byUser);

    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));
    when(sessionRepository.findActiveByInterview(organizationId, interviewId))
        .thenReturn(Mono.empty());
    when(sessionRepository.save(any(InterviewSession.class)))
        .thenAnswer(
            invocation -> {
              InterviewSession session = invocation.getArgument(0);
              if (session.getId() == null) {
                session.setId(UUID.randomUUID());
              }
              return Mono.just(session);
            });
    when(interviewRepository.save(any(Interview.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(eventPublisher.publishStarted(any(InterviewSession.class))).thenReturn(Mono.empty());

    StepVerifier.create(
            sessionService.start(organizationId, interviewId, "hash", "device-1", "1.0", 5, byUser))
        .assertNext(
            session -> {
              assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
              assertThat(session.getStartedAt()).isNotNull();
              assertThat(session.getInterviewId()).isEqualTo(interviewId);
            })
        .verifyComplete();

    verify(eventPublisher).publishStarted(any(InterviewSession.class));
  }

  @Test
  void startRejectsSecondActiveSession() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Interview interview = interview(organizationId, interviewId, byUser);

    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));
    when(sessionRepository.findActiveByInterview(organizationId, interviewId))
        .thenReturn(Mono.just(session(organizationId, interviewId, UUID.randomUUID())));

    StepVerifier.create(
            sessionService.start(organizationId, interviewId, "hash", null, null, 5, byUser))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void completeEndsSessionAndPublishesCompletedEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    InterviewSession session = session(organizationId, interviewId, sessionId);
    session.start();
    Interview interview = interview(organizationId, interviewId, byUser);

    when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
    when(sessionRepository.save(any(InterviewSession.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(interviewRepository.findLiveById(interviewId)).thenReturn(Mono.just(interview));
    when(interviewRepository.save(any(Interview.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(eventPublisher.publishCompleted(any(InterviewSession.class))).thenReturn(Mono.empty());

    StepVerifier.create(sessionService.complete(sessionId, organizationId, byUser))
        .assertNext(
            result -> {
              assertThat(result.getStatus()).isEqualTo(SessionStatus.ENDED);
              assertThat(result.getEndedAt()).isNotNull();
            })
        .verifyComplete();

    verify(eventPublisher).publishCompleted(any(InterviewSession.class));
  }

  @Test
  void pausePausesActiveSession() {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    InterviewSession session = session(organizationId, UUID.randomUUID(), sessionId);
    session.start();

    when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
    when(sessionRepository.save(any(InterviewSession.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sessionService.pause(sessionId, organizationId))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(SessionStatus.PAUSED))
        .verifyComplete();
  }

  private static InterviewSession session(UUID organizationId, UUID interviewId, UUID sessionId) {
    InterviewSession session =
        new InterviewSession(organizationId, interviewId, "hash", "device-1", "1.0", 5);
    session.setId(sessionId);
    return session;
  }

  private static Interview interview(UUID organizationId, UUID interviewId, UUID byUser) {
    Instant startsAt = Instant.now();
    Interview interview =
        new Interview(
            organizationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
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
