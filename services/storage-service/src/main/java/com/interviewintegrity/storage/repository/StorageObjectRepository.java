package com.interviewintegrity.storage.repository;

import com.interviewintegrity.storage.domain.StorageClass;
import com.interviewintegrity.storage.domain.StorageObject;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link StorageObject} entities. */
public interface StorageObjectRepository extends ReactiveCrudRepository<StorageObject, UUID> {

  String SELECT_LIVE_BY_ORGANIZATION =
      "SELECT * FROM storage_objects WHERE organization_id = :organizationId ";

  /** Finds a live object by id. */
  @Query("SELECT * FROM storage_objects WHERE id = :id AND deleted_at IS NULL")
  Mono<StorageObject> findLiveById(UUID id);

  /** Finds a live object of an organization in a bucket by key. */
  @Query(
      SELECT_LIVE_BY_ORGANIZATION
          + "AND bucket_id = :bucketId AND key = :key AND deleted_at IS NULL LIMIT 1")
  Mono<StorageObject> findLiveByOrganizationAndBucketAndKey(
      UUID organizationId, UUID bucketId, String key);

  /** Lists the live objects of an organization, newest first. */
  @Query(SELECT_LIVE_BY_ORGANIZATION + "AND deleted_at IS NULL ORDER BY uploaded_at DESC")
  Flux<StorageObject> listLiveByOrganization(UUID organizationId);

  /** Lists the live objects of an organization in a bucket, newest first. */
  @Query(
      SELECT_LIVE_BY_ORGANIZATION
          + "AND bucket_id = :bucketId AND deleted_at IS NULL ORDER BY uploaded_at DESC")
  Flux<StorageObject> listLiveByBucket(UUID organizationId, UUID bucketId);

  /** Lists the live objects of an organization in a storage class, newest first. */
  @Query(
      SELECT_LIVE_BY_ORGANIZATION
          + "AND storage_class = :storageClass AND deleted_at IS NULL ORDER BY uploaded_at DESC")
  Flux<StorageObject> listLiveByStorageClass(UUID organizationId, StorageClass storageClass);

  /** Resolves whether a key already exists within a bucket. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM storage_objects WHERE bucket_id = :bucketId "
          + "AND key = :key AND deleted_at IS NULL)")
  Mono<Boolean> existsByBucketAndKey(UUID bucketId, String key);
}
