package com.integrity.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.exception.ConflictException;
import com.integrity.interview.domain.FeedbackStatus;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewFeedback;
import com.integrity.interview.domain.InterviewMode;
import com.integrity.interview.domain.Interviewer;
import com.integrity.interview.repository.InterviewFeedbackRepository;
import com.integrity.interview.repository.InterviewRepository;
import com.integrity.interview.repository.InterviewerRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the interview feedback service. */
@ExtendWith(MockitoExtension.class)
class InterviewFeedbackServiceTest {

  @Mock private InterviewFeedbackRepository feedbackRepository;
  @Mock private InterviewRepository interviewRepository;
  @Mock private InterviewerRepository interviewerRepository;

  private InterviewFeedbackService feedbackService;

  @BeforeEach
  void setUp() {
    feedbackService =
        new InterviewFeedbackService(
            feedbackRepository, interviewRepository, interviewerRepository);
  }

  @Test
  void createSavesDraftFeedback() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID interviewerId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();

    when(interviewRepository.findLiveById(interviewId))
        .thenReturn(Mono.just(interview(organizationId, interviewId)));
    when(interviewerRepository.findLiveById(interviewerId))
        .thenReturn(Mono.just(interviewer(organizationId, interviewerId)));
    when(feedbackRepository.findLiveByOrganizationAndInterviewAndInterviewer(
            organizationId, interviewId, interviewerId))
        .thenReturn(Mono.empty());
    when(feedbackRepository.save(any(InterviewFeedback.class)))
        .thenAnswer(
            invocation -> {
              InterviewFeedback feedback = invocation.getArgument(0);
              feedback.setId(UUID.randomUUID());
              return Mono.just(feedback);
            });

    StepVerifier.create(feedbackService.create(organizationId, interviewId, interviewerId, byUser))
        .assertNext(
            feedback -> {
              assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.DRAFT);
              assertThat(feedback.getInterviewerId()).isEqualTo(interviewerId);
            })
        .verifyComplete();
  }

  @Test
  void createRejectsDuplicateFeedback() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID interviewerId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();

    when(interviewRepository.findLiveById(interviewId))
        .thenReturn(Mono.just(interview(organizationId, interviewId)));
    when(interviewerRepository.findLiveById(interviewerId))
        .thenReturn(Mono.just(interviewer(organizationId, interviewerId)));
    when(feedbackRepository.findLiveByOrganizationAndInterviewAndInterviewer(
            organizationId, interviewId, interviewerId))
        .thenReturn(
            Mono.just(new InterviewFeedback(organizationId, interviewId, interviewerId, byUser)));

    StepVerifier.create(feedbackService.create(organizationId, interviewId, interviewerId, byUser))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void submitFinalizesFeedback() {
    UUID organizationId = UUID.randomUUID();
    UUID interviewId = UUID.randomUUID();
    UUID interviewerId = UUID.randomUUID();
    UUID feedbackId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    InterviewFeedback feedback =
        new InterviewFeedback(organizationId, interviewId, interviewerId, byUser);
    feedback.setId(feedbackId);

    when(feedbackRepository.findLiveById(feedbackId)).thenReturn(Mono.just(feedback));
    when(feedbackRepository.save(any(InterviewFeedback.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(feedbackService.submit(feedbackId, organizationId, byUser))
        .assertNext(
            result -> {
              assertThat(result.getStatus()).isEqualTo(FeedbackStatus.SUBMITTED);
              assertThat(result.getSubmittedAt()).isNotNull();
            })
        .verifyComplete();
  }

  private static Interview interview(UUID organizationId, UUID interviewId) {
    Instant startsAt = Instant.now();
    Interview interview =
        new Interview(
            organizationId,
            UUID.randomUUID(),
            "candidate@example.com",
            "Test Candidate",
            UUID.randomUUID(),
            1,
            "Phone screen",
            InterviewMode.ONLINE,
            null,
            startsAt,
            startsAt.plusSeconds(1800),
            "UTC",
            null,
            UUID.randomUUID());
    interview.setId(interviewId);
    return interview;
  }

  private static Interviewer interviewer(UUID organizationId, UUID interviewerId) {
    Interviewer interviewer =
        new Interviewer(
            organizationId,
            UUID.randomUUID(),
            "Ada Lovelace",
            "ada@example.com",
            UUID.randomUUID());
    interviewer.setId(interviewerId);
    return interviewer;
  }
}
