package com.integrity.interview.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.Interviewer;
import com.integrity.interview.repository.InterviewPanelRepository;
import com.integrity.interview.repository.InterviewRepository;
import com.integrity.interview.repository.InterviewerRepository;
import com.integrity.interview.web.dto.InterviewPanelResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the interviewer panels of interviews. */
public class InterviewPanelService {

  private final InterviewPanelRepository panelRepository;
  private final InterviewRepository interviewRepository;
  private final InterviewerRepository interviewerRepository;
  private final InterviewMapper mapper;

  /** Wires the service with its repositories and mapper. */
  public InterviewPanelService(
      InterviewPanelRepository panelRepository,
      InterviewRepository interviewRepository,
      InterviewerRepository interviewerRepository,
      InterviewMapper mapper) {
    this.panelRepository = panelRepository;
    this.interviewRepository = interviewRepository;
    this.interviewerRepository = interviewerRepository;
    this.mapper = mapper;
  }

  /** Adds an interviewer to the panel of an interview. */
  @Transactional
  public Mono<InterviewPanelResponse> addPanelist(
      UUID organizationId, UUID interviewId, UUID interviewerId, String role, UUID addedBy) {
    return requireInterview(organizationId, interviewId)
        .then(requireInterviewer(organizationId, interviewerId))
        .then(panelRepository.exists(interviewId, interviewerId))
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Interviewer already on the panel"));
              }
              return panelRepository.add(interviewId, interviewerId, role, addedBy);
            })
        .thenReturn(
            new InterviewPanelResponse(interviewId, interviewerId, role, addedBy, Instant.now()));
  }

  /** Removes an interviewer from the panel of an interview. */
  @Transactional
  public Mono<Void> removePanelist(UUID organizationId, UUID interviewId, UUID interviewerId) {
    return requireInterview(organizationId, interviewId)
        .then(panelRepository.remove(interviewId, interviewerId));
  }

  /** Lists the panel of an interview. */
  @Transactional(readOnly = true)
  public Flux<InterviewPanelResponse> listPanel(UUID organizationId, UUID interviewId) {
    return requireInterview(organizationId, interviewId)
        .thenMany(panelRepository.listByInterview(interviewId).map(mapper::toResponse));
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
}
