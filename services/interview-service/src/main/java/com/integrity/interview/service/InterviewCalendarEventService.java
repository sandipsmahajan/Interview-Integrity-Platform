package com.integrity.interview.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewCalendarEvent;
import com.integrity.interview.repository.InterviewCalendarEventRepository;
import com.integrity.interview.repository.InterviewRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the external calendar event mirrors of interviews. */
public class InterviewCalendarEventService {

  private final InterviewCalendarEventRepository calendarRepository;
  private final InterviewRepository interviewRepository;

  /** Wires the service with its repositories. */
  public InterviewCalendarEventService(
      InterviewCalendarEventRepository calendarRepository,
      InterviewRepository interviewRepository) {
    this.calendarRepository = calendarRepository;
    this.interviewRepository = interviewRepository;
  }

  /** Creates a calendar event mirror, rejecting a duplicate provider event. */
  @Transactional
  public Mono<InterviewCalendarEvent> create(
      UUID organizationId,
      UUID interviewId,
      String provider,
      String providerEventId,
      String eventUrl,
      Instant startsAt,
      Instant endsAt) {
    return requireInterview(organizationId, interviewId)
        .then(
            calendarRepository.findByOrganizationProviderAndEvent(
                organizationId, provider, providerEventId))
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("Calendar event already exists for provider"));
              }
              return calendarRepository.save(
                  new InterviewCalendarEvent(
                      organizationId,
                      interviewId,
                      provider,
                      providerEventId,
                      eventUrl,
                      startsAt,
                      endsAt));
            });
  }

  /** Lists the calendar events of an interview. */
  @Transactional(readOnly = true)
  public Flux<InterviewCalendarEvent> list(UUID organizationId, UUID interviewId) {
    return requireInterview(organizationId, interviewId)
        .thenMany(calendarRepository.listByOrganizationAndInterview(organizationId, interviewId));
  }

  /** Updates the mirror of a calendar event. */
  @Transactional
  public Mono<InterviewCalendarEvent> update(
      UUID eventId,
      UUID organizationId,
      String eventUrl,
      Instant startsAt,
      Instant endsAt,
      String status) {
    return requireOwned(eventId, organizationId)
        .map(
            event -> {
              event.update(eventUrl, startsAt, endsAt, status);
              return event;
            })
        .flatMap(calendarRepository::save);
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

  private Mono<InterviewCalendarEvent> requireOwned(UUID eventId, UUID organizationId) {
    return calendarRepository
        .findById(eventId)
        .switchIfEmpty(Mono.error(new NotFoundException("Calendar event not found")))
        .flatMap(
            event -> {
              if (!organizationId.equals(event.getOrganizationId())) {
                return Mono.error(new NotFoundException("Calendar event not found"));
              }
              return Mono.just(event);
            });
  }
}
