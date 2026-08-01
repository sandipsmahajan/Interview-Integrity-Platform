package com.interviewintegrity.audit.service;

import com.interviewintegrity.api.PageResponse;
import com.interviewintegrity.api.PageResponses;
import com.interviewintegrity.audit.domain.AuditEvent;
import com.interviewintegrity.audit.domain.AuditEventChange;
import com.interviewintegrity.audit.domain.AuditOutcome;
import com.interviewintegrity.audit.repository.AuditEventChangeRepository;
import com.interviewintegrity.audit.repository.AuditEventRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Provides search and retrieval over the append-only compliance audit trail. */
public class AuditService {

  private final AuditEventRepository auditEventRepository;
  private final AuditEventChangeRepository changeRepository;

  /** Wires the service with its repositories. */
  public AuditService(
      AuditEventRepository auditEventRepository, AuditEventChangeRepository changeRepository) {
    this.auditEventRepository = auditEventRepository;
    this.changeRepository = changeRepository;
  }

  /** Appends a new compliance audit event. */
  @Transactional
  public Mono<AuditEvent> record(AuditEvent event) {
    return auditEventRepository.save(event);
  }

  /** Searches the audit events of an organization with the given filters. */
  @Transactional(readOnly = true)
  public Mono<PageResponse<AuditEvent>> search(
      UUID organizationId,
      UUID actorId,
      String resourceType,
      UUID resourceId,
      AuditOutcome outcome,
      Pageable pageable) {
    Flux<AuditEvent> page;
    Mono<Long> count;
    if (resourceType != null && resourceId != null) {
      page =
          auditEventRepository.listByResource(organizationId, resourceType, resourceId, pageable);
      count = auditEventRepository.countByResource(organizationId, resourceType, resourceId);
    } else if (actorId != null) {
      page = auditEventRepository.listByActor(organizationId, actorId, pageable);
      count = auditEventRepository.countByActor(organizationId, actorId);
    } else if (outcome != null) {
      page = auditEventRepository.listByOutcome(organizationId, outcome, pageable);
      count = auditEventRepository.countByOutcome(organizationId, outcome);
    } else {
      page = auditEventRepository.listByOrganization(organizationId, pageable);
      count = auditEventRepository.countByOrganization(organizationId);
    }
    return page.collectList()
        .zipWith(count)
        .map(
            tuple ->
                PageResponses.of(
                    tuple.getT1(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    tuple.getT2()));
  }

  /** Returns a single audit event of an organization. */
  @Transactional(readOnly = true)
  public Mono<AuditEvent> get(UUID eventId, UUID organizationId) {
    return auditEventRepository
        .findByIdAndOrganization(eventId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Audit event not found")));
  }

  /** Lists the field-level changes of an audit event. */
  @Transactional(readOnly = true)
  public Flux<AuditEventChange> changes(UUID eventId, UUID organizationId) {
    return auditEventRepository
        .findByIdAndOrganization(eventId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Audit event not found")))
        .thenMany(changeRepository.listByAuditEventId(eventId));
  }
}
