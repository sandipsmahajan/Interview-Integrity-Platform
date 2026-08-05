package com.integrity.storage.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.storage.domain.ObjectVersion;
import com.integrity.storage.domain.StorageBucket;
import com.integrity.storage.domain.StorageClass;
import com.integrity.storage.domain.StorageObject;
import com.integrity.storage.domain.StorageObjectHistory;
import com.integrity.storage.repository.ObjectVersionRepository;
import com.integrity.storage.repository.StorageBucketRepository;
import com.integrity.storage.repository.StorageObjectHistoryRepository;
import com.integrity.storage.repository.StorageObjectRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages object metadata and its version history. */
public class StorageObjectService {

  private static final String OBJECT_NOT_FOUND = "Object not found";

  private final StorageBucketRepository bucketRepository;
  private final StorageObjectRepository objectRepository;
  private final ObjectVersionRepository versionRepository;
  private final StorageObjectHistoryRepository historyRepository;

  /** Wires the service with its repositories. */
  public StorageObjectService(
      StorageBucketRepository bucketRepository,
      StorageObjectRepository objectRepository,
      ObjectVersionRepository versionRepository,
      StorageObjectHistoryRepository historyRepository) {
    this.bucketRepository = bucketRepository;
    this.objectRepository = objectRepository;
    this.versionRepository = versionRepository;
    this.historyRepository = historyRepository;
  }

  /** Registers an object and its initial version, rejecting duplicate keys. */
  @Transactional
  public Mono<StorageObject> register(
      UUID organizationId,
      UUID bucketId,
      String key,
      long sizeBytes,
      String contentType,
      String checksumSha256,
      StorageClass storageClass,
      String storageRef,
      String metadata,
      UUID uploadedBy) {
    return bucketRepository
        .findLiveById(bucketId)
        .switchIfEmpty(Mono.error(new NotFoundException("Bucket not found")))
        .flatMap(bucket -> assertBucketOrganization(bucket, organizationId))
        .then(
            Mono.defer(
                () ->
                    objectRepository
                        .existsByBucketAndKey(bucketId, key)
                        .flatMap(
                            exists -> {
                              if (exists) {
                                return Mono.error(
                                    new ConflictException("Object already exists in bucket"));
                              }
                              return objectRepository
                                  .save(
                                      new StorageObject(
                                          organizationId,
                                          bucketId,
                                          key,
                                          sizeBytes,
                                          contentType,
                                          checksumSha256,
                                          storageClass,
                                          storageRef,
                                          metadata,
                                          uploadedBy))
                                  .flatMap(
                                      object ->
                                          versionRepository
                                              .save(
                                                  new ObjectVersion(
                                                      object.getId(),
                                                      organizationId,
                                                      1,
                                                      storageRef,
                                                      sizeBytes,
                                                      checksumSha256,
                                                      uploadedBy))
                                              .thenReturn(object));
                            })));
  }

  /** Lists the live objects of an organization, optionally scoped by bucket or storage class. */
  @Transactional(readOnly = true)
  public Flux<StorageObject> list(UUID organizationId, UUID bucketId, StorageClass storageClass) {
    if (bucketId != null) {
      return bucketRepository
          .findLiveById(bucketId)
          .switchIfEmpty(Mono.error(new NotFoundException("Bucket not found")))
          .flatMap(bucket -> assertBucketOrganization(bucket, organizationId))
          .thenMany(objectRepository.listLiveByBucket(organizationId, bucketId));
    }
    if (storageClass != null) {
      return objectRepository.listLiveByStorageClass(organizationId, storageClass);
    }
    return objectRepository.listLiveByOrganization(organizationId);
  }

  /** Returns a single object of an organization. */
  @Transactional(readOnly = true)
  public Mono<StorageObject> get(UUID objectId, UUID organizationId) {
    return objectRepository
        .findLiveById(objectId)
        .switchIfEmpty(Mono.error(new NotFoundException(OBJECT_NOT_FOUND)))
        .flatMap(object -> assertObjectOrganization(object, organizationId));
  }

  /** Updates the mutable metadata of an object. */
  @Transactional
  public Mono<StorageObject> update(
      UUID objectId,
      UUID organizationId,
      String contentType,
      StorageClass storageClass,
      String metadata,
      UUID byUser) {
    return objectRepository
        .findLiveById(objectId)
        .switchIfEmpty(Mono.error(new NotFoundException(OBJECT_NOT_FOUND)))
        .flatMap(object -> assertObjectOrganization(object, organizationId))
        .map(
            object -> {
              object.update(contentType, storageClass, metadata, byUser);
              return object;
            })
        .flatMap(objectRepository::save);
  }

  /** Soft deletes an object. */
  @Transactional
  public Mono<Void> delete(UUID objectId, UUID organizationId, UUID byUser) {
    return objectRepository
        .findLiveById(objectId)
        .switchIfEmpty(Mono.error(new NotFoundException(OBJECT_NOT_FOUND)))
        .flatMap(object -> assertObjectOrganization(object, organizationId))
        .map(
            object -> {
              object.delete(byUser);
              return object;
            })
        .flatMap(objectRepository::save)
        .then();
  }

  /** Lists the active versions of an object. */
  @Transactional(readOnly = true)
  public Flux<ObjectVersion> listVersions(UUID objectId, UUID organizationId) {
    return get(objectId, organizationId)
        .thenMany(versionRepository.listByOrganizationAndObject(objectId, organizationId));
  }

  /** Lists the history snapshots of an object. */
  @Transactional(readOnly = true)
  public Flux<StorageObjectHistory> listHistory(UUID objectId, UUID organizationId) {
    return get(objectId, organizationId)
        .thenMany(historyRepository.listByOrganizationAndObject(organizationId, objectId));
  }

  private Mono<StorageBucket> assertBucketOrganization(StorageBucket bucket, UUID organizationId) {
    if (!organizationId.equals(bucket.getOrganizationId())) {
      return Mono.error(new NotFoundException("Bucket not found"));
    }
    return Mono.just(bucket);
  }

  private Mono<StorageObject> assertObjectOrganization(StorageObject object, UUID organizationId) {
    if (!organizationId.equals(object.getOrganizationId())) {
      return Mono.error(new NotFoundException(OBJECT_NOT_FOUND));
    }
    return Mono.just(object);
  }
}
