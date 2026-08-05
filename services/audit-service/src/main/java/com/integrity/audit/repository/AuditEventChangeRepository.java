package com.integrity.audit.repository;

import com.integrity.audit.domain.AuditEventChange;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link AuditEventChange} entities. */
public interface AuditEventChangeRepository extends ReactiveCrudRepository<AuditEventChange, Long> {

  /** Lists the changes recorded for an audit event. */
  @Query("SELECT * FROM audit_event_changes WHERE audit_event_id = :auditEventId " + "ORDER BY id")
  Flux<AuditEventChange> listByAuditEventId(UUID auditEventId);
}
