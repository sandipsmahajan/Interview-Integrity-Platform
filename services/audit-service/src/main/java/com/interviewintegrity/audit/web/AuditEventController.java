package com.interviewintegrity.audit.web;

import com.interviewintegrity.api.PageResponse;
import com.interviewintegrity.api.PageResponses;
import com.interviewintegrity.audit.domain.AuditEvent;
import com.interviewintegrity.audit.domain.AuditEventChange;
import com.interviewintegrity.audit.domain.AuditOutcome;
import com.interviewintegrity.audit.service.AuditService;
import com.interviewintegrity.audit.web.dto.AuditEventChangeResponse;
import com.interviewintegrity.audit.web.dto.AuditEventResponse;
import com.interviewintegrity.audit.web.dto.CreateAuditEventRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Compliance audit trail endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit Events", description = "Compliance audit trail")
public final class AuditEventController {

  private final AuditService auditService;

  /** Creates the controller bound to the audit service. */
  public AuditEventController(AuditService auditService) {
    this.auditService = auditService;
  }

  /** Records a new compliance audit event. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an audit event")
  public Mono<AuditEventResponse> record(
      Authentication authentication, @Valid @RequestBody CreateAuditEventRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    Instant occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
    AuditEvent event =
        new AuditEvent(
            organizationId,
            request.actorId(),
            request.actorType(),
            request.action().trim(),
            request.resourceType().trim(),
            request.resourceId(),
            request.outcome(),
            occurredAt,
            request.requestId(),
            request.ipAddress(),
            request.userAgent(),
            request.metadata());
    return auditService.record(event).map(this::toResponse);
  }

  /** Searches the audit events of the organization. */
  @GetMapping
  @Operation(summary = "Search audit events")
  public Mono<PageResponse<AuditEventResponse>> search(
      Authentication authentication,
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) String resourceType,
      @RequestParam(required = false) UUID resourceId,
      @RequestParam(required = false) AuditOutcome outcome,
      Pageable pageable) {
    return auditService
        .search(
            SecurityPrincipals.organizationId(authentication),
            actorId,
            resourceType,
            resourceId,
            outcome,
            pageable)
        .map(
            page ->
                PageResponses.of(
                    page.items().stream().map(this::toResponse).toList(),
                    page.page(),
                    page.size(),
                    page.totalElements()));
  }

  /** Returns a single audit event. */
  @GetMapping("/{eventId}")
  @Operation(summary = "Get an audit event")
  public Mono<AuditEventResponse> get(Authentication authentication, @PathVariable UUID eventId) {
    return auditService
        .get(eventId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Lists the field-level changes of an audit event. */
  @GetMapping("/{eventId}/changes")
  @Operation(summary = "List audit event changes")
  public Flux<AuditEventChangeResponse> changes(
      Authentication authentication, @PathVariable UUID eventId) {
    return auditService
        .changes(eventId, SecurityPrincipals.organizationId(authentication))
        .map(this::toChangeResponse);
  }

  private AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(
        event.getId(),
        event.getOrganizationId(),
        event.getActorId(),
        event.getActorType(),
        event.getAction(),
        event.getResourceType(),
        event.getResourceId(),
        event.getOutcome(),
        event.getOccurredAt(),
        event.getRequestId(),
        event.getIpAddress(),
        event.getUserAgent(),
        event.getMetadata());
  }

  private AuditEventChangeResponse toChangeResponse(AuditEventChange change) {
    return new AuditEventChangeResponse(
        change.getId(),
        change.getAuditEventId(),
        change.getOccurredAt(),
        change.getField(),
        change.getOldValue(),
        change.getNewValue());
  }
}
