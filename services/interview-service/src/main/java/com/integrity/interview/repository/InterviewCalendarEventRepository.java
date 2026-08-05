package com.integrity.interview.repository;

import com.integrity.interview.domain.InterviewCalendarEvent;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link InterviewCalendarEvent} entities. */
public interface InterviewCalendarEventRepository
    extends ReactiveCrudRepository<InterviewCalendarEvent, UUID> {

  /** Finds a calendar event by id. */
  @Override
  Mono<InterviewCalendarEvent> findById(UUID id);

  /** Lists the calendar events of an interview by start time. */
  @Query(
      "SELECT * FROM interview_calendar_events WHERE organization_id = :organizationId "
          + "AND interview_id = :interviewId ORDER BY starts_at")
  Flux<InterviewCalendarEvent> listByOrganizationAndInterview(
      UUID organizationId, UUID interviewId);

  /** Finds a calendar event by provider and provider event id. */
  @Query(
      "SELECT * FROM interview_calendar_events WHERE organization_id = :organizationId "
          + "AND provider = :provider AND provider_event_id = :providerEventId LIMIT 1")
  Mono<InterviewCalendarEvent> findByOrganizationProviderAndEvent(
      UUID organizationId, String provider, String providerEventId);
}
