package com.integrity.storage.repository;

import com.integrity.storage.domain.ObjectVersion;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link ObjectVersion} entities. */
public interface ObjectVersionRepository extends ReactiveCrudRepository<ObjectVersion, Long> {

  /** Lists the active versions of an object, newest first. */
  @Query(
      "SELECT * FROM object_versions WHERE object_id = :objectId "
          + "AND organization_id = :organizationId AND deleted_at IS NULL ORDER BY version DESC")
  Flux<ObjectVersion> listByOrganizationAndObject(UUID objectId, UUID organizationId);

  /** Finds the newest active version of an object. */
  @Query(
      "SELECT * FROM object_versions WHERE object_id = :objectId "
          + "AND organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY version DESC LIMIT 1")
  Mono<ObjectVersion> findLatestByOrganizationAndObject(UUID objectId, UUID organizationId);
}
