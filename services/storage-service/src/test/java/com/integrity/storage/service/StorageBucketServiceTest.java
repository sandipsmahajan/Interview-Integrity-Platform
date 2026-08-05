package com.integrity.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.storage.domain.StorageBucket;
import com.integrity.storage.repository.StorageBucketRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the storage bucket service. */
@ExtendWith(MockitoExtension.class)
class StorageBucketServiceTest {

  @Mock private StorageBucketRepository bucketRepository;

  private StorageBucketService bucketService;

  @BeforeEach
  void setUp() {
    bucketService = new StorageBucketService(bucketRepository);
  }

  @Test
  void createDeclaresNewBucket() {
    UUID organizationId = UUID.randomUUID();
    when(bucketRepository.existsByOrganizationAndName(organizationId, "assets"))
        .thenReturn(Mono.just(false));
    when(bucketRepository.save(any(StorageBucket.class)))
        .thenAnswer(
            invocation -> {
              StorageBucket bucket = invocation.getArgument(0);
              bucket.setId(UUID.randomUUID());
              return Mono.just(bucket);
            });

    StepVerifier.create(
            bucketService.create(organizationId, "assets", true, "{}", UUID.randomUUID()))
        .assertNext(
            bucket -> {
              assertThat(bucket.getName()).isEqualTo("assets");
              assertThat(bucket.isVersioningEnabled()).isTrue();
              assertThat(bucket.getOrganizationId()).isEqualTo(organizationId);
            })
        .verifyComplete();
  }

  @Test
  void createRejectsDuplicateName() {
    UUID organizationId = UUID.randomUUID();
    when(bucketRepository.existsByOrganizationAndName(organizationId, "assets"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            bucketService.create(organizationId, "assets", true, null, UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void listReturnsBucketsOfOrganization() {
    UUID organizationId = UUID.randomUUID();
    StorageBucket bucket =
        new StorageBucket(organizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(UUID.randomUUID());
    when(bucketRepository.listLiveByOrganization(organizationId)).thenReturn(Flux.just(bucket));

    StepVerifier.create(bucketService.list(organizationId))
        .assertNext(result -> assertThat(result.getName()).isEqualTo("assets"))
        .verifyComplete();
  }

  @Test
  void getReturnsBucket() {
    UUID organizationId = UUID.randomUUID();
    UUID bucketId = UUID.randomUUID();
    StorageBucket bucket =
        new StorageBucket(organizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(bucketId);

    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.just(bucket));

    StepVerifier.create(bucketService.get(bucketId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(bucketId))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID bucketId = UUID.randomUUID();
    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.empty());

    StepVerifier.create(bucketService.get(bucketId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID foreignOrganizationId = UUID.randomUUID();
    UUID bucketId = UUID.randomUUID();
    StorageBucket bucket =
        new StorageBucket(foreignOrganizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(bucketId);

    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.just(bucket));

    StepVerifier.create(bucketService.get(bucketId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateReplacesBucket() {
    UUID organizationId = UUID.randomUUID();
    UUID bucketId = UUID.randomUUID();
    StorageBucket bucket =
        new StorageBucket(organizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(bucketId);

    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.just(bucket));
    when(bucketRepository.save(any(StorageBucket.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            bucketService.update(
                bucketId, organizationId, "reports", false, "{}", UUID.randomUUID()))
        .assertNext(
            result -> {
              assertThat(result.getName()).isEqualTo("reports");
              assertThat(result.isVersioningEnabled()).isFalse();
            })
        .verifyComplete();
  }

  @Test
  void deleteSoftDeletesBucket() {
    UUID organizationId = UUID.randomUUID();
    UUID bucketId = UUID.randomUUID();
    StorageBucket bucket =
        new StorageBucket(organizationId, "assets", true, "{}", UUID.randomUUID());
    bucket.setId(bucketId);

    when(bucketRepository.findLiveById(bucketId)).thenReturn(Mono.just(bucket));
    when(bucketRepository.save(any(StorageBucket.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(bucketService.delete(bucketId, organizationId, UUID.randomUUID()))
        .verifyComplete();
  }
}
