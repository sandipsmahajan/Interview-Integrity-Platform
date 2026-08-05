package com.integrity.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the storage object service. */
@ExtendWith(MockitoExtension.class)
class StorageObjectServiceTest {

  @Mock private StorageBucketRepository bucketRepository;
  @Mock private StorageObjectRepository objectRepository;
  @Mock private ObjectVersionRepository versionRepository;
  @Mock private StorageObjectHistoryRepository historyRepository;

  private StorageObjectService objectService;

  @BeforeEach
  void setUp() {
    objectService =
        new StorageObjectService(
            bucketRepository, objectRepository, versionRepository, historyRepository);
  }

  private StorageBucket liveBucket(UUID organizationId) {
    StorageBucket bucket =
        new StorageBucket(organizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(UUID.randomUUID());
    return bucket;
  }

  @Test
  void registerCreatesObjectAndInitialVersion() {
    UUID organizationId = UUID.randomUUID();
    StorageBucket bucket = liveBucket(organizationId);
    when(bucketRepository.findLiveById(bucket.getId())).thenReturn(Mono.just(bucket));
    when(objectRepository.existsByBucketAndKey(bucket.getId(), "reports/q1.pdf"))
        .thenReturn(Mono.just(false));
    when(objectRepository.save(any(StorageObject.class)))
        .thenAnswer(
            invocation -> {
              StorageObject object = invocation.getArgument(0);
              object.setId(UUID.randomUUID());
              return Mono.just(object);
            });
    when(versionRepository.save(any(ObjectVersion.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            objectService.register(
                organizationId,
                bucket.getId(),
                "reports/q1.pdf",
                1024,
                "application/pdf",
                "abc",
                StorageClass.STANDARD,
                "s3://assets/reports/q1.pdf",
                "{}",
                UUID.randomUUID()))
        .assertNext(
            object -> {
              assertThat(object.getKey()).isEqualTo("reports/q1.pdf");
              assertThat(object.getBucketId()).isEqualTo(bucket.getId());
              assertThat(object.getSizeBytes()).isEqualTo(1024);
            })
        .verifyComplete();
  }

  @Test
  void registerRejectsDuplicateKey() {
    UUID organizationId = UUID.randomUUID();
    StorageBucket bucket = liveBucket(organizationId);
    when(bucketRepository.findLiveById(bucket.getId())).thenReturn(Mono.just(bucket));
    when(objectRepository.existsByBucketAndKey(bucket.getId(), "reports/q1.pdf"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            objectService.register(
                organizationId,
                bucket.getId(),
                "reports/q1.pdf",
                1024,
                null,
                null,
                StorageClass.STANDARD,
                "s3://assets/reports/q1.pdf",
                null,
                UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void registerReturnsNotFoundForUnknownBucket() {
    UUID bucketId = UUID.randomUUID();
    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.empty());

    StepVerifier.create(
            objectService.register(
                UUID.randomUUID(),
                bucketId,
                "reports/q1.pdf",
                1024,
                null,
                null,
                StorageClass.STANDARD,
                "s3://assets/reports/q1.pdf",
                null,
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void registerRejectsCrossTenantBucket() {
    UUID organizationId = UUID.randomUUID();
    StorageBucket foreignBucket = liveBucket(UUID.randomUUID());
    when(bucketRepository.findLiveById(foreignBucket.getId())).thenReturn(Mono.just(foreignBucket));

    StepVerifier.create(
            objectService.register(
                organizationId,
                foreignBucket.getId(),
                "reports/q1.pdf",
                1024,
                null,
                null,
                StorageClass.STANDARD,
                "s3://assets/reports/q1.pdf",
                null,
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listAllReturnsObjectsOfOrganization() {
    UUID organizationId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    when(objectRepository.listLiveByOrganization(organizationId)).thenReturn(Flux.just(object));

    StepVerifier.create(objectService.list(organizationId, null, null))
        .assertNext(result -> assertThat(result.getKey()).isEqualTo("reports/q1.pdf"))
        .verifyComplete();
  }

  @Test
  void listByBucketReturnsObjectsOfBucket() {
    UUID organizationId = UUID.randomUUID();
    StorageBucket bucket = liveBucket(organizationId);
    StorageObject object = liveObject(organizationId);
    when(bucketRepository.findLiveById(bucket.getId())).thenReturn(Mono.just(bucket));
    when(objectRepository.listLiveByBucket(organizationId, bucket.getId()))
        .thenReturn(Flux.just(object));

    StepVerifier.create(objectService.list(organizationId, bucket.getId(), null))
        .assertNext(result -> assertThat(result.getKey()).isEqualTo("reports/q1.pdf"))
        .verifyComplete();
  }

  @Test
  void listByStorageClassReturnsObjectsOfClass() {
    UUID organizationId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    when(objectRepository.listLiveByStorageClass(organizationId, StorageClass.ARCHIVE))
        .thenReturn(Flux.just(object));

    StepVerifier.create(objectService.list(organizationId, null, StorageClass.ARCHIVE))
        .assertNext(result -> assertThat(result.getStorageClass()).isEqualTo(StorageClass.STANDARD))
        .verifyComplete();
  }

  @Test
  void getReturnsObject() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    object.setId(objectId);

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));

    StepVerifier.create(objectService.get(objectId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(objectId))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID objectId = UUID.randomUUID();
    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.empty());

    StepVerifier.create(objectService.get(objectId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(UUID.randomUUID());
    object.setId(objectId);

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));

    StepVerifier.create(objectService.get(objectId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateReplacesMetadata() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    object.setId(objectId);

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));
    when(objectRepository.save(any(StorageObject.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            objectService.update(
                objectId,
                organizationId,
                "text/plain",
                StorageClass.INFREQUENT,
                "{\"owner\":\"ops\"}",
                UUID.randomUUID()))
        .assertNext(
            result -> {
              assertThat(result.getContentType()).isEqualTo("text/plain");
              assertThat(result.getStorageClass()).isEqualTo(StorageClass.INFREQUENT);
            })
        .verifyComplete();
  }

  @Test
  void deleteSoftDeletesObject() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    object.setId(objectId);

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));
    when(objectRepository.save(any(StorageObject.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(objectService.delete(objectId, organizationId, UUID.randomUUID()))
        .verifyComplete();
  }

  @Test
  void listVersionsListsVersionsOfObject() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    object.setId(objectId);
    ObjectVersion version =
        new ObjectVersion(
            objectId,
            organizationId,
            1,
            "s3://assets/reports/q1.pdf",
            1024,
            "abc",
            UUID.randomUUID());

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));
    when(versionRepository.listByOrganizationAndObject(objectId, organizationId))
        .thenReturn(Flux.just(version));

    StepVerifier.create(objectService.listVersions(objectId, organizationId))
        .assertNext(result -> assertThat(result.getVersion()).isEqualTo(1))
        .verifyComplete();
  }

  @Test
  void listHistoryListsSnapshotsOfObject() {
    UUID organizationId = UUID.randomUUID();
    UUID objectId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    object.setId(objectId);
    StorageObjectHistory history =
        new StorageObjectHistory(
            "INSERT",
            UUID.randomUUID(),
            Instant.now(),
            objectId,
            organizationId,
            object.getBucketId(),
            "reports/q1.pdf",
            1024L,
            "application/pdf",
            StorageClass.STANDARD,
            1L);

    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.just(object));
    when(historyRepository.listByOrganizationAndObject(organizationId, objectId))
        .thenReturn(Flux.just(history));

    StepVerifier.create(objectService.listHistory(objectId, organizationId))
        .assertNext(result -> assertThat(result.getHistoryAction()).isEqualTo("INSERT"))
        .verifyComplete();
  }

  private StorageObject liveObject(UUID organizationId) {
    return new StorageObject(
        organizationId,
        UUID.randomUUID(),
        "reports/q1.pdf",
        1024,
        "application/pdf",
        "abc",
        StorageClass.STANDARD,
        "s3://assets/reports/q1.pdf",
        "{}",
        UUID.randomUUID());
  }
}
