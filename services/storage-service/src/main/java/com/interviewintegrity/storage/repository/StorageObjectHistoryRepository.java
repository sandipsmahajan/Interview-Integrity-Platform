package com.interviewintegrity.storage.repository;

import com.interviewintegrity.storage.domain.StorageObjectHistory;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for {@link StorageObjectHistory} entities. */
public interface StorageObjectHistoryRepository
    extends ReactiveCrudRepository<StorageObjectHistory, Long> {

  /** Lists the history snapshots of an object, newest first. */
  @Query(
      "SELECT * FROM storage_objects_history WHERE organization_id = :organizationId "
          + "AND id = :objectId ORDER BY changed_at DESC")
  Flux<StorageObjectHistory> listByOrganizationAndObject(UUID organizationId, UUID objectId);
}
