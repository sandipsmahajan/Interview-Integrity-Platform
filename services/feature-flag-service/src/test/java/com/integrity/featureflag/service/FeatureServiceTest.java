package com.integrity.featureflag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.featureflag.domain.Feature;
import com.integrity.featureflag.domain.FlagKind;
import com.integrity.featureflag.repository.FeatureRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the feature service. */
@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

  @Mock private FeatureRepository featureRepository;

  private FeatureService featureService;

  @BeforeEach
  void setUp() {
    featureService = new FeatureService(featureRepository);
  }

  @Test
  void createDeclaresNewFeature() {
    UUID organizationId = UUID.randomUUID();
    when(featureRepository.existsByOrganizationAndCode(organizationId, "analytics.dashboard"))
        .thenReturn(Mono.just(false));
    when(featureRepository.save(any(Feature.class)))
        .thenAnswer(
            invocation -> {
              Feature feature = invocation.getArgument(0);
              feature.setId(UUID.randomUUID());
              return Mono.just(feature);
            });

    StepVerifier.create(
            featureService.create(
                organizationId,
                "analytics.dashboard",
                "Analytics Dashboard",
                "Enables the analytics dashboard",
                FlagKind.BOOLEAN,
                UUID.randomUUID()))
        .assertNext(
            feature -> {
              assertThat(feature.getCode()).isEqualTo("analytics.dashboard");
              assertThat(feature.getKind()).isEqualTo(FlagKind.BOOLEAN);
              assertThat(feature.getOrganizationId()).isEqualTo(organizationId);
            })
        .verifyComplete();
  }

  @Test
  void createRejectsDuplicateCode() {
    UUID organizationId = UUID.randomUUID();
    when(featureRepository.existsByOrganizationAndCode(organizationId, "analytics.dashboard"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            featureService.create(
                organizationId,
                "analytics.dashboard",
                "Analytics Dashboard",
                null,
                FlagKind.BOOLEAN,
                UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void listReturnsFeaturesOfOrganization() {
    UUID organizationId = UUID.randomUUID();
    Feature feature =
        new Feature(
            organizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    when(featureRepository.listLiveByOrganization(organizationId)).thenReturn(Flux.just(feature));

    StepVerifier.create(featureService.list(organizationId))
        .assertNext(result -> assertThat(result.getCode()).isEqualTo("analytics.dashboard"))
        .verifyComplete();
  }

  @Test
  void getReturnsFeature() {
    UUID organizationId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    Feature feature =
        new Feature(
            organizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    feature.setId(featureId);

    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.just(feature));

    StepVerifier.create(featureService.get(featureId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(featureId))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID featureId = UUID.randomUUID();
    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.empty());

    StepVerifier.create(featureService.get(featureId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void getRejectsCrossTenantAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID foreignOrganizationId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    Feature feature =
        new Feature(
            foreignOrganizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    feature.setId(featureId);

    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.just(feature));

    StepVerifier.create(featureService.get(featureId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateReplacesFeature() {
    UUID organizationId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    Feature feature =
        new Feature(
            organizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    feature.setId(featureId);

    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.just(feature));
    when(featureRepository.save(any(Feature.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            featureService.update(
                featureId, organizationId, "Reports", "Enables the reports", UUID.randomUUID()))
        .assertNext(
            result -> {
              assertThat(result.getName()).isEqualTo("Reports");
              assertThat(result.getDescription()).isEqualTo("Enables the reports");
            })
        .verifyComplete();
  }

  @Test
  void deleteSoftDeletesFeature() {
    UUID organizationId = UUID.randomUUID();
    UUID featureId = UUID.randomUUID();
    Feature feature =
        new Feature(
            organizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    feature.setId(featureId);

    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.just(feature));
    when(featureRepository.save(any(Feature.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(featureService.delete(featureId, organizationId, UUID.randomUUID()))
        .verifyComplete();
  }
}
