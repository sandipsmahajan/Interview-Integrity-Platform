package com.interviewintegrity.storage.repository;

import com.interviewintegrity.storage.domain.StorageBucket;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link StorageBucket} entities. */
public interface StorageBucketRepository extends ReactiveCrudRepository<StorageBucket, UUID> {

  /** Finds a live bucket by id. */
  @Query("SELECT * FROM storage_buckets WHERE id = :id AND deleted_at IS NULL")
  Mono<StorageBucket> findLiveById(UUID id);

  /** Finds a live bucket of an organization by name. */
  @Query(
      "SELECT * FROM storage_buckets WHERE organization_id = :organizationId AND name = :name "
          + "AND deleted_at IS NULL LIMIT 1")
  Mono<StorageBucket> findLiveByOrganizationAndName(UUID organizationId, String name);

  /** Lists the live buckets of an organization. */
  @Query(
      "SELECT * FROM storage_buckets WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY name")
  Flux<StorageBucket> listLiveByOrganization(UUID organizationId);

  /** Resolves whether a bucket name already exists within an organization. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM storage_buckets WHERE organization_id = :organizationId "
          + "AND name = :name AND deleted_at IS NULL)")
  Mono<Boolean> existsByOrganizationAndName(UUID organizationId, String name);
}
