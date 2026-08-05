package com.integrity.interview.web;

import com.integrity.interview.service.InterviewCalendarEventService;
import com.integrity.interview.service.InterviewMapper;
import com.integrity.interview.web.dto.CreateCalendarEventRequest;
import com.integrity.interview.web.dto.InterviewCalendarEventResponse;
import com.integrity.interview.web.dto.UpdateCalendarEventRequest;
import com.integrity.security.SecurityPrincipals;
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
  private final InterviewMapper mapper;

  /** Creates the controller bound to the calendar event service and mapper. */
  public InterviewCalendarEventController(
      InterviewCalendarEventService calendarService, InterviewMapper mapper) {
    this.calendarService = calendarService;
    this.mapper = mapper;
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
        .map(mapper::toResponse);
  }

  /** Lists the calendar event mirrors of an interview. */
  @GetMapping
  @Operation(summary = "List calendar event mirrors")
  public Flux<InterviewCalendarEventResponse> list(
      Authentication authentication, @PathVariable UUID interviewId) {
    return calendarService
        .list(SecurityPrincipals.organizationId(authentication), interviewId)
        .map(mapper::toResponse);
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
        .map(mapper::toResponse);
  }
}
