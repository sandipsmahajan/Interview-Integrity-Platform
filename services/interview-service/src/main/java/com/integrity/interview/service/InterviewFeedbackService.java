package com.integrity.interview.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewFeedback;
import com.integrity.interview.domain.Interviewer;
import com.integrity.interview.repository.InterviewFeedbackRepository;
import com.integrity.interview.repository.InterviewRepository;
import com.integrity.interview.repository.InterviewerRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the structured feedback collected for interviews. */
public class InterviewFeedbackService {

  private final InterviewFeedbackRepository feedbackRepository;
  private final InterviewRepository interviewRepository;
  private final InterviewerRepository interviewerRepository;

  /** Wires the service with its repositories. */
  public InterviewFeedbackService(
      InterviewFeedbackRepository feedbackRepository,
      InterviewRepository interviewRepository,
      InterviewerRepository interviewerRepository) {
    this.feedbackRepository = feedbackRepository;
    this.interviewRepository = interviewRepository;
    this.interviewerRepository = interviewerRepository;
  }

  /** Creates a draft feedback record for an interviewer. */
  @Transactional
  public Mono<InterviewFeedback> create(
      UUID organizationId, UUID interviewId, UUID interviewerId, UUID createdBy) {
    return requireInterview(organizationId, interviewId)
        .then(requireInterviewer(organizationId, interviewerId))
        .then(
            feedbackRepository.findLiveByOrganizationAndInterviewAndInterviewer(
                organizationId, interviewId, interviewerId))
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Feedback already exists for this interviewer"));
              }
              return feedbackRepository.save(
                  new InterviewFeedback(organizationId, interviewId, interviewerId, createdBy));
            });
  }

  /** Lists the live feedback of an interview. */
  @Transactional(readOnly = true)
  public Flux<InterviewFeedback> list(UUID organizationId, UUID interviewId) {
    return requireInterview(organizationId, interviewId)
        .thenMany(
            feedbackRepository.listLiveByOrganizationAndInterview(organizationId, interviewId));
  }

  /** Updates the body of a draft feedback record. */
  @Transactional
  public Mono<InterviewFeedback> update(
      UUID feedbackId,
      UUID organizationId,
      Integer rating,
      String strengths,
      String concerns,
      String recommendation,
      UUID byUser) {
    return requireOwned(feedbackId, organizationId)
        .map(
            feedback -> {
              feedback.update(rating, strengths, concerns, recommendation, byUser);
              return feedback;
            })
        .flatMap(feedbackRepository::save);
  }

  /** Submits a draft feedback record. */
  @Transactional
  public Mono<InterviewFeedback> submit(UUID feedbackId, UUID organizationId, UUID byUser) {
    return requireOwned(feedbackId, organizationId)
        .map(
            feedback -> {
              feedback.submit(byUser);
              return feedback;
            })
        .flatMap(feedbackRepository::save);
  }

  /** Soft deletes a feedback record. */
  @Transactional
  public Mono<Void> delete(UUID feedbackId, UUID organizationId, UUID byUser) {
    return requireOwned(feedbackId, organizationId)
        .map(
            feedback -> {
              feedback.delete(byUser);
              return feedback;
            })
        .flatMap(feedbackRepository::save)
        .then();
  }

  private Mono<Interview> requireInterview(UUID organizationId, UUID interviewId) {
    return interviewRepository
        .findLiveById(interviewId)
        .switchIfEmpty(Mono.error(new NotFoundException("Interview not found")))
        .flatMap(
            interview -> {
              if (!organizationId.equals(interview.getOrganizationId())) {
                return Mono.error(new NotFoundException("Interview not found"));
              }
              return Mono.just(interview);
            });
  }

  private Mono<Interviewer> requireInterviewer(UUID organizationId, UUID interviewerId) {
    return interviewerRepository
        .findLiveById(interviewerId)
        .switchIfEmpty(Mono.error(new NotFoundException("Interviewer not found")))
        .flatMap(
            interviewer -> {
              if (!organizationId.equals(interviewer.getOrganizationId())) {
                return Mono.error(new NotFoundException("Interviewer not found"));
              }
              return Mono.just(interviewer);
            });
  }

  private Mono<InterviewFeedback> requireOwned(UUID feedbackId, UUID organizationId) {
    return feedbackRepository
        .findLiveById(feedbackId)
        .switchIfEmpty(Mono.error(new NotFoundException("Feedback not found")))
        .flatMap(
            feedback -> {
              if (!organizationId.equals(feedback.getOrganizationId())) {
                return Mono.error(new NotFoundException("Feedback not found"));
              }
              return Mono.just(feedback);
            });
  }
}
