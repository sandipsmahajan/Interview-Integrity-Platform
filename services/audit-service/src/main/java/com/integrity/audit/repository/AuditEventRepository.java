package com.integrity.audit.repository;

import com.integrity.audit.domain.AuditEvent;
import com.integrity.audit.domain.AuditOutcome;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link AuditEvent} entities. */
public interface AuditEventRepository extends ReactiveCrudRepository<AuditEvent, UUID> {

  String SELECT_BY_ORGANIZATION =
      "SELECT * FROM audit_events WHERE organization_id = :organizationId ";

  /** Finds an audit event by id within an organization. */
  @Query("SELECT * FROM audit_events WHERE id = :id AND organization_id = :organizationId")
  Mono<AuditEvent> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the audit events of an organization, newest first. */
  @Query(SELECT_BY_ORGANIZATION + "ORDER BY occurred_at DESC")
  Flux<AuditEvent> listByOrganization(UUID organizationId, Pageable pageable);

  /** Lists the audit events for a resource, newest first. */
  @Query(
      SELECT_BY_ORGANIZATION
          + "AND resource_type = :resourceType AND resource_id = :resourceId "
          + "ORDER BY occurred_at DESC")
  Flux<AuditEvent> listByResource(
      UUID organizationId, String resourceType, UUID resourceId, Pageable pageable);

  /** Lists the audit events performed by an actor, newest first. */
  @Query(SELECT_BY_ORGANIZATION + "AND actor_id = :actorId ORDER BY occurred_at DESC")
  Flux<AuditEvent> listByActor(UUID organizationId, UUID actorId, Pageable pageable);

  /** Lists the audit events with the given outcome, newest first. */
  @Query(SELECT_BY_ORGANIZATION + "AND outcome = :outcome ORDER BY occurred_at DESC")
  Flux<AuditEvent> listByOutcome(UUID organizationId, AuditOutcome outcome, Pageable pageable);

  /** Counts the audit events of an organization. */
  @Query("SELECT count(*) FROM audit_events WHERE organization_id = :organizationId")
  Mono<Long> countByOrganization(UUID organizationId);

  /** Counts the audit events for a resource. */
  @Query(
      "SELECT count(*) FROM audit_events WHERE organization_id = :organizationId "
          + "AND resource_type = :resourceType AND resource_id = :resourceId")
  Mono<Long> countByResource(UUID organizationId, String resourceType, UUID resourceId);

  /** Counts the audit events performed by an actor. */
  @Query(
      "SELECT count(*) FROM audit_events WHERE organization_id = :organizationId "
          + "AND actor_id = :actorId")
  Mono<Long> countByActor(UUID organizationId, UUID actorId);

  /** Counts the audit events with the given outcome. */
  @Query(
      "SELECT count(*) FROM audit_events WHERE organization_id = :organizationId "
          + "AND outcome = :outcome")
  Mono<Long> countByOutcome(UUID organizationId, AuditOutcome outcome);
}
