package com.interviewintegrity.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.storage.domain.SignedUrl;
import com.interviewintegrity.storage.domain.StorageClass;
import com.interviewintegrity.storage.domain.StorageObject;
import com.interviewintegrity.storage.domain.UrlPurpose;
import com.interviewintegrity.storage.repository.SignedUrlRepository;
import com.interviewintegrity.storage.repository.StorageObjectRepository;
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

/** Unit tests for the signed URL service. */
@ExtendWith(MockitoExtension.class)
class SignedUrlServiceTest {

  @Mock private StorageObjectRepository objectRepository;
  @Mock private SignedUrlRepository signedUrlRepository;

  private SignedUrlService signedUrlService;

  @BeforeEach
  void setUp() {
    signedUrlService = new SignedUrlService(objectRepository, signedUrlRepository);
  }

  private StorageObject liveObject(UUID organizationId) {
    StorageObject object =
        new StorageObject(
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
    object.setId(UUID.randomUUID());
    return object;
  }

  @Test
  void createIssuesGrantWithRawToken() {
    UUID organizationId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    Instant expiresAt = Instant.now().plusSeconds(3600);
    when(objectRepository.findLiveById(object.getId())).thenReturn(Mono.just(object));
    when(signedUrlRepository.save(any(SignedUrl.class)))
        .thenAnswer(
            invocation -> {
              SignedUrl signedUrl = invocation.getArgument(0);
              signedUrl.setId(UUID.randomUUID());
              return Mono.just(signedUrl);
            });

    StepVerifier.create(
            signedUrlService.create(
                object.getId(),
                organizationId,
                UrlPurpose.DOWNLOAD,
                expiresAt,
                5,
                UUID.randomUUID()))
        .assertNext(
            grant -> {
              assertThat(grant.signedUrl().getPurpose()).isEqualTo(UrlPurpose.DOWNLOAD);
              assertThat(grant.signedUrl().getExpiresAt()).isEqualTo(expiresAt);
              assertThat(grant.signedUrl().getMaxUses()).isEqualTo(5);
              assertThat(grant.signedUrl().getTokenHash()).hasSize(64);
              assertThat(grant.token()).hasSize(64);
              assertThat(grant.token()).isNotEqualTo(grant.signedUrl().getTokenHash());
            })
        .verifyComplete();
  }

  @Test
  void createReturnsNotFoundForUnknownObject() {
    UUID objectId = UUID.randomUUID();
    when(objectRepository.findLiveById(objectId)).thenReturn(Mono.empty());

    StepVerifier.create(
            signedUrlService.create(
                objectId,
                UUID.randomUUID(),
                UrlPurpose.DOWNLOAD,
                Instant.now().plusSeconds(3600),
                null,
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void createRejectsCrossTenantObject() {
    UUID organizationId = UUID.randomUUID();
    StorageObject foreignObject = liveObject(UUID.randomUUID());
    when(objectRepository.findLiveById(foreignObject.getId())).thenReturn(Mono.just(foreignObject));

    StepVerifier.create(
            signedUrlService.create(
                foreignObject.getId(),
                organizationId,
                UrlPurpose.DOWNLOAD,
                Instant.now().plusSeconds(3600),
                null,
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listListsGrantsOfObject() {
    UUID organizationId = UUID.randomUUID();
    StorageObject object = liveObject(organizationId);
    SignedUrl signedUrl =
        new SignedUrl(
            organizationId,
            object.getId(),
            UrlPurpose.UPLOAD,
            "hash",
            Instant.now().plusSeconds(3600),
            null,
            UUID.randomUUID());
    signedUrl.setId(UUID.randomUUID());

    when(objectRepository.findLiveById(object.getId())).thenReturn(Mono.just(object));
    when(signedUrlRepository.listByObjectAndOrganization(object.getId(), organizationId))
        .thenReturn(Flux.just(signedUrl));

    StepVerifier.create(signedUrlService.list(object.getId(), organizationId))
        .assertNext(result -> assertThat(result.getPurpose()).isEqualTo(UrlPurpose.UPLOAD))
        .verifyComplete();
  }

  @Test
  void getReturnsGrant() {
    UUID organizationId = UUID.randomUUID();
    UUID urlId = UUID.randomUUID();
    SignedUrl signedUrl =
        new SignedUrl(
            organizationId,
            UUID.randomUUID(),
            UrlPurpose.DOWNLOAD,
            "hash",
            Instant.now().plusSeconds(3600),
            null,
            UUID.randomUUID());
    signedUrl.setId(urlId);

    when(signedUrlRepository.findByIdAndOrganization(urlId, organizationId))
        .thenReturn(Mono.just(signedUrl));

    StepVerifier.create(signedUrlService.get(urlId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(urlId))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID urlId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(signedUrlRepository.findByIdAndOrganization(urlId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(signedUrlService.get(urlId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void revokeMarksGrantRevoked() {
    UUID organizationId = UUID.randomUUID();
    UUID urlId = UUID.randomUUID();
    SignedUrl signedUrl =
        new SignedUrl(
            organizationId,
            UUID.randomUUID(),
            UrlPurpose.DELETE,
            "hash",
            Instant.now().plusSeconds(3600),
            null,
            UUID.randomUUID());
    signedUrl.setId(urlId);

    when(signedUrlRepository.findByIdAndOrganization(urlId, organizationId))
        .thenReturn(Mono.just(signedUrl));
    when(signedUrlRepository.save(any(SignedUrl.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(signedUrlService.revoke(urlId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getRevokedAt()).isNotNull())
        .verifyComplete();
  }
}
