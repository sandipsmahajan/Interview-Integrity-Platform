package com.interviewintegrity.interview.web;

import com.interviewintegrity.interview.domain.InterviewCalendarEvent;
import com.interviewintegrity.interview.service.InterviewCalendarEventService;
import com.interviewintegrity.interview.web.dto.CreateCalendarEventRequest;
import com.interviewintegrity.interview.web.dto.InterviewCalendarEventResponse;
import com.interviewintegrity.interview.web.dto.UpdateCalendarEventRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Calendar event mirror endpoints. */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/calendar-events")
@Tag(name = "Interview Calendar Events", description = "Manage calendar event mirrors")
public final class InterviewCalendarEventController {

  private final InterviewCalendarEventService calendarService;

  /** Creates the controller bound to the calendar event service. */
  public InterviewCalendarEventController(InterviewCalendarEventService calendarService) {
    this.calendarService = calendarService;
  }

  /** Creates a calendar event mirror. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a calendar event mirror")
  public Mono<InterviewCalendarEventResponse> create(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody CreateCalendarEventRequest request) {
    return calendarService
        .create(
            SecurityPrincipals.organizationId(authentication),
            interviewId,
            request.provider().trim(),
            request.providerEventId().trim(),
            request.eventUrl(),
            request.startsAt(),
            request.endsAt())
        .map(this::toResponse);
  }

  /** Lists the calendar event mirrors of an interview. */
  @GetMapping
  @Operation(summary = "List calendar event mirrors")
  public Flux<InterviewCalendarEventResponse> list(
      Authentication authentication, @PathVariable UUID interviewId) {
    return calendarService
        .list(SecurityPrincipals.organizationId(authentication), interviewId)
        .map(this::toResponse);
  }

  /** Updates a calendar event mirror. */
  @PatchMapping("/{eventId}")
  @Operation(summary = "Update a calendar event mirror")
  public Mono<InterviewCalendarEventResponse> update(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @PathVariable UUID eventId,
      @Valid @RequestBody UpdateCalendarEventRequest request) {
    return calendarService
        .update(
            eventId,
            SecurityPrincipals.organizationId(authentication),
            request.eventUrl(),
            request.startsAt(),
            request.endsAt(),
            request.status())
        .map(this::toResponse);
  }

  private InterviewCalendarEventResponse toResponse(InterviewCalendarEvent event) {
    return new InterviewCalendarEventResponse(
        event.getId(),
        event.getOrganizationId(),
        event.getInterviewId(),
        event.getProvider(),
        event.getProviderEventId(),
        event.getEventUrl(),
        event.getStartsAt(),
        event.getEndsAt(),
        event.getStatus(),
        event.getCreatedAt(),
        event.getUpdatedAt());
  }
}
