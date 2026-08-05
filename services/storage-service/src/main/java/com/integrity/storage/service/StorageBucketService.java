package com.integrity.storage.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.storage.domain.StorageBucket;
import com.integrity.storage.repository.StorageBucketRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages tenant scoped storage buckets. */
public class StorageBucketService {

  private static final String BUCKET_NOT_FOUND = "Bucket not found";

  private final StorageBucketRepository bucketRepository;

  /** Wires the service with its repository. */
  public StorageBucketService(StorageBucketRepository bucketRepository) {
    this.bucketRepository = bucketRepository;
  }

  /** Creates a bucket, rejecting duplicate names. */
  @Transactional
  public Mono<StorageBucket> create(
      UUID organizationId, String name, boolean versioningEnabled, String policy, UUID createdBy) {
    return bucketRepository
        .existsByOrganizationAndName(organizationId, name)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Bucket already exists"));
              }
              return bucketRepository.save(
                  new StorageBucket(organizationId, name, versioningEnabled, policy, createdBy));
            });
  }

  /** Lists the buckets of an organization. */
  @Transactional(readOnly = true)
  public Flux<StorageBucket> list(UUID organizationId) {
    return bucketRepository.listLiveByOrganization(organizationId);
  }

  /** Returns a single bucket of an organization. */
  @Transactional(readOnly = true)
  public Mono<StorageBucket> get(UUID bucketId, UUID organizationId) {
    return bucketRepository
        .findLiveById(bucketId)
        .switchIfEmpty(Mono.error(new NotFoundException(BUCKET_NOT_FOUND)))
        .flatMap(bucket -> assertOrganization(bucket, organizationId));
  }

  /** Updates a bucket. */
  @Transactional
  public Mono<StorageBucket> update(
      UUID bucketId,
      UUID organizationId,
      String name,
      boolean versioningEnabled,
      String policy,
      UUID byUser) {
    return bucketRepository
        .findLiveById(bucketId)
        .switchIfEmpty(Mono.error(new NotFoundException(BUCKET_NOT_FOUND)))
        .flatMap(bucket -> assertOrganization(bucket, organizationId))
        .map(
            bucket -> {
              bucket.update(name, versioningEnabled, policy, byUser);
              return bucket;
            })
        .flatMap(bucketRepository::save);
  }

  /** Soft deletes a bucket. */
  @Transactional
  public Mono<Void> delete(UUID bucketId, UUID organizationId, UUID byUser) {
    return bucketRepository
        .findLiveById(bucketId)
        .switchIfEmpty(Mono.error(new NotFoundException(BUCKET_NOT_FOUND)))
        .flatMap(bucket -> assertOrganization(bucket, organizationId))
        .map(
            bucket -> {
              bucket.delete(byUser);
              return bucket;
            })
        .flatMap(bucketRepository::save)
        .then();
  }

  private Mono<StorageBucket> assertOrganization(StorageBucket bucket, UUID organizationId) {
    if (!organizationId.equals(bucket.getOrganizationId())) {
      return Mono.error(new NotFoundException(BUCKET_NOT_FOUND));
    }
    return Mono.just(bucket);
  }
}
