package com.interviewintegrity.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.interviewintegrity.api.PageResponse;
import com.interviewintegrity.audit.domain.AuditEvent;
import com.interviewintegrity.audit.domain.AuditEventChange;
import com.interviewintegrity.audit.domain.AuditOutcome;
import com.interviewintegrity.audit.repository.AuditEventChangeRepository;
import com.interviewintegrity.audit.repository.AuditEventRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the audit trail service. */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  private static final String RESOURCE_TYPE_CANDIDATE = "CANDIDATE";

  @Mock private AuditEventRepository auditEventRepository;
  @Mock private AuditEventChangeRepository changeRepository;

  private AuditService auditService;

  @BeforeEach
  void setUp() {
    auditService = new AuditService(auditEventRepository, changeRepository);
  }

  @Test
  void recordSavesAuditEvent() {
    UUID organizationId = UUID.randomUUID();
    AuditEvent event = auditEvent(organizationId, UUID.randomUUID());

    when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(Mono.just(event));

    StepVerifier.create(auditService.record(event))
        .assertNext(saved -> assertThat(saved.getOrganizationId()).isEqualTo(organizationId))
        .verifyComplete();
  }

  @Test
  void searchListsEventsAndCountsForOrganization() {
    UUID organizationId = UUID.randomUUID();
    AuditEvent event = auditEvent(organizationId, UUID.randomUUID());
    PageRequest pageable = PageRequest.of(0, 20);

    when(auditEventRepository.listByOrganization(organizationId, pageable))
        .thenReturn(Flux.just(event));
    when(auditEventRepository.countByOrganization(organizationId)).thenReturn(Mono.just(1L));

    StepVerifier.create(auditService.search(organizationId, null, null, null, null, pageable))
        .assertNext(
            page -> {
              assertThat(page.totalElements()).isEqualTo(1);
              assertThat(page.items()).hasSize(1);
              assertThat(page.page()).isZero();
            })
        .verifyComplete();
  }

  @Test
  void searchFiltersByActor() {
    UUID organizationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    AuditEvent event = auditEvent(organizationId, actorId);
    PageRequest pageable = PageRequest.of(0, 10);

    when(auditEventRepository.listByActor(organizationId, actorId, pageable))
        .thenReturn(Flux.just(event));
    when(auditEventRepository.countByActor(organizationId, actorId)).thenReturn(Mono.just(1L));

    StepVerifier.create(auditService.search(organizationId, actorId, null, null, null, pageable))
        .assertNext(PageResponse::items)
        .verifyComplete();
  }

  @Test
  void searchFiltersByResource() {
    UUID organizationId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    AuditEvent event = auditEvent(organizationId, resourceId);
    PageRequest pageable = PageRequest.of(0, 10);

    when(auditEventRepository.listByResource(
            organizationId, RESOURCE_TYPE_CANDIDATE, resourceId, pageable))
        .thenReturn(Flux.just(event));
    when(auditEventRepository.countByResource(organizationId, RESOURCE_TYPE_CANDIDATE, resourceId))
        .thenReturn(Mono.just(1L));

    StepVerifier.create(
            auditService.search(
                organizationId, null, RESOURCE_TYPE_CANDIDATE, resourceId, null, pageable))
        .assertNext(page -> assertThat(page.totalElements()).isEqualTo(1))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID organizationId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    when(auditEventRepository.findByIdAndOrganization(eventId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(auditService.get(eventId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void changesListsChangeRecordsAfterVerifyingEvent() {
    UUID organizationId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    AuditEvent event = auditEvent(organizationId, eventId);
    AuditEventChange change = new AuditEventChange(eventId, Instant.now(), "name", "old", "new");

    when(auditEventRepository.findByIdAndOrganization(eventId, organizationId))
        .thenReturn(Mono.just(event));
    when(changeRepository.listByAuditEventId(eventId)).thenReturn(Flux.just(change));

    StepVerifier.create(auditService.changes(eventId, organizationId))
        .assertNext(result -> assertThat(result.getField()).isEqualTo("name"))
        .verifyComplete();
  }

  @Test
  void changesRejectsCrossTenantEvent() {
    UUID eventId = UUID.randomUUID();
    when(auditEventRepository.findByIdAndOrganization(eq(eventId), any())).thenReturn(Mono.empty());

    StepVerifier.create(auditService.changes(eventId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void searchReturnsEmptyPageWhenNoEvents() {
    UUID organizationId = UUID.randomUUID();
    PageRequest pageable = PageRequest.of(0, 20);

    when(auditEventRepository.listByOrganization(organizationId, pageable))
        .thenReturn(Flux.empty());
    when(auditEventRepository.countByOrganization(organizationId)).thenReturn(Mono.just(0L));

    StepVerifier.create(auditService.search(organizationId, null, null, null, null, pageable))
        .assertNext(
            page -> {
              assertThat(page.items()).isEqualTo(List.of());
              assertThat(page.totalPages()).isZero();
            })
        .verifyComplete();
  }

  private static AuditEvent auditEvent(UUID organizationId, UUID eventId) {
    AuditEvent event =
        new AuditEvent(
            organizationId,
            UUID.randomUUID(),
            "USER",
            "PROFILE_UPDATED",
            RESOURCE_TYPE_CANDIDATE,
            UUID.randomUUID(),
            AuditOutcome.SUCCESS,
            Instant.now(),
            "req-1",
            "10.0.0.1",
            "agent",
            "{}");
    event.setId(eventId);
    return event;
  }
}
