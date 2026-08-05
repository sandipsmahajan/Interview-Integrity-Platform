package com.integrity.audit.repository;

import com.integrity.audit.domain.ApiAuditLog;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ApiAuditLog} entities. */
public interface ApiAuditLogRepository extends ReactiveCrudRepository<ApiAuditLog, Long> {

  /** Lists the access log entries of an organization, newest first. */
  @Query(
      "SELECT * FROM api_audit_log WHERE organization_id = :organizationId "
          + "ORDER BY occurred_at DESC")
  Flux<ApiAuditLog> listByOrganization(UUID organizationId, Pageable pageable);

  /** Lists the access log entries of an organization for a method, newest first. */
  @Query(
      "SELECT * FROM api_audit_log WHERE organization_id = :organizationId "
          + "AND method = :method ORDER BY occurred_at DESC")
  Flux<ApiAuditLog> listByOrganizationAndMethod(
      UUID organizationId, String method, Pageable pageable);

  /** Counts the access log entries of an organization. */
  @Query("SELECT count(*) FROM api_audit_log WHERE organization_id = :organizationId")
  Mono<Long> countByOrganization(UUID organizationId);

  /** Counts the access log entries of an organization for a method. */
  @Query(
      "SELECT count(*) FROM api_audit_log WHERE organization_id = :organizationId "
          + "AND method = :method")
  Mono<Long> countByOrganizationAndMethod(UUID organizationId, String method);
}
