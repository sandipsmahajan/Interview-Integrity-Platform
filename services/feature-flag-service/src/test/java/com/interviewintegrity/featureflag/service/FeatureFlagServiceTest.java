package com.interviewintegrity.featureflag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.featureflag.domain.Feature;
import com.interviewintegrity.featureflag.domain.FeatureFlag;
import com.interviewintegrity.featureflag.domain.FeatureFlagHistory;
import com.interviewintegrity.featureflag.domain.FlagKind;
import com.interviewintegrity.featureflag.domain.FlagTarget;
import com.interviewintegrity.featureflag.repository.FeatureFlagHistoryRepository;
import com.interviewintegrity.featureflag.repository.FeatureFlagRepository;
import com.interviewintegrity.featureflag.repository.FeatureRepository;
import com.interviewintegrity.featureflag.repository.FlagTargetRepository;
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

/** Unit tests for the feature flag service. */
@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

  @Mock private FeatureRepository featureRepository;
  @Mock private FeatureFlagRepository flagRepository;
  @Mock private FlagTargetRepository targetRepository;
  @Mock private FeatureFlagHistoryRepository historyRepository;

  private FeatureFlagService flagService;

  @BeforeEach
  void setUp() {
    flagService =
        new FeatureFlagService(
            featureRepository, flagRepository, targetRepository, historyRepository);
  }

  private Feature liveFeature(UUID organizationId) {
    Feature feature =
        new Feature(
            organizationId,
            "analytics.dashboard",
            "Analytics Dashboard",
            null,
            FlagKind.BOOLEAN,
            UUID.randomUUID());
    feature.setId(UUID.randomUUID());
    return feature;
  }

  @Test
  void createFlagDeclaresConfiguration() {
    UUID organizationId = UUID.randomUUID();
    Feature feature = liveFeature(organizationId);
    when(featureRepository.findLiveById(feature.getId())).thenReturn(Mono.just(feature));
    when(flagRepository.existsByFeatureIdAndEnvironment(feature.getId(), "PRODUCTION"))
        .thenReturn(Mono.just(false));
    when(flagRepository.save(any(FeatureFlag.class)))
        .thenAnswer(
            invocation -> {
              FeatureFlag flag = invocation.getArgument(0);
              flag.setId(UUID.randomUUID());
              return Mono.just(flag);
            });

    StepVerifier.create(
            flagService.createFlag(
                organizationId,
                feature.getId(),
                "PRODUCTION",
                true,
                50,
                "control",
                "{}",
                "{}",
                UUID.randomUUID()))
        .assertNext(
            flag -> {
              assertThat(flag.getEnvironment()).isEqualTo("PRODUCTION");
              assertThat(flag.getRolloutPercent()).isEqualTo(50);
            })
        .verifyComplete();
  }

  @Test
  void createFlagRejectsDuplicateEnvironment() {
    UUID organizationId = UUID.randomUUID();
    Feature feature = liveFeature(organizationId);
    when(featureRepository.findLiveById(feature.getId())).thenReturn(Mono.just(feature));
    when(flagRepository.existsByFeatureIdAndEnvironment(feature.getId(), "PRODUCTION"))
        .thenReturn(Mono.just(true));

    StepVerifier.create(
            flagService.createFlag(
                organizationId,
                feature.getId(),
                "PRODUCTION",
                true,
                100,
                "control",
                "{}",
                "{}",
                UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void createFlagReturnsNotFoundForUnknownFeature() {
    UUID featureId = UUID.randomUUID();
    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.empty());

    StepVerifier.create(
            flagService.createFlag(
                UUID.randomUUID(),
                featureId,
                "PRODUCTION",
                true,
                100,
                null,
                null,
                null,
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listFlagsListsFlagsOfFeature() {
    UUID organizationId = UUID.randomUUID();
    Feature feature = liveFeature(organizationId);
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            feature.getId(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(UUID.randomUUID());

    when(featureRepository.findLiveById(feature.getId())).thenReturn(Mono.just(feature));
    when(flagRepository.listByOrganizationAndFeature(organizationId, feature.getId()))
        .thenReturn(Flux.just(flag));

    StepVerifier.create(flagService.listFlags(organizationId, feature.getId()))
        .assertNext(result -> assertThat(result.getEnvironment()).isEqualTo("PRODUCTION"))
        .verifyComplete();
  }

  @Test
  void getFlagReturnsFlag() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));

    StepVerifier.create(flagService.getFlag(flagId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(flagId))
        .verifyComplete();
  }

  @Test
  void getFlagReturnsNotFoundForUnknownId() {
    UUID flagId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(flagRepository.findByIdAndOrganization(flagId, organizationId)).thenReturn(Mono.empty());

    StepVerifier.create(flagService.getFlag(flagId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void updateFlagReplacesConfiguration() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));
    when(flagRepository.save(any(FeatureFlag.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            flagService.updateFlag(
                flagId, organizationId, false, 10, "treatment", "{}", "{}", UUID.randomUUID()))
        .assertNext(
            result -> {
              assertThat(result.isEnabled()).isFalse();
              assertThat(result.getRolloutPercent()).isEqualTo(10);
              assertThat(result.getDefaultVariant()).isEqualTo("treatment");
            })
        .verifyComplete();
  }

  @Test
  void addTargetUpsertsOverride() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));
    when(targetRepository.upsert(
            any(UUID.class), any(UUID.class), any(), anyBoolean(), any(UUID.class)))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            flagService.addTarget(
                flagId, organizationId, userId, "treatment", true, UUID.randomUUID()))
        .assertNext(
            target -> {
              assertThat(target.getFlagId()).isEqualTo(flagId);
              assertThat(target.getUserId()).isEqualTo(userId);
              assertThat(target.getVariant()).isEqualTo("treatment");
            })
        .verifyComplete();
  }

  @Test
  void removeTargetRemovesOverride() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));
    when(targetRepository.remove(flagId, userId)).thenReturn(Mono.empty());

    StepVerifier.create(flagService.removeTarget(flagId, organizationId, userId)).verifyComplete();
  }

  @Test
  void listTargetsListsOverrides() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    FlagTarget target =
        new FlagTarget(flagId, UUID.randomUUID(), "treatment", true, UUID.randomUUID());
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));
    when(targetRepository.listByFlag(flagId)).thenReturn(Flux.just(target));

    StepVerifier.create(flagService.listTargets(flagId, organizationId))
        .assertNext(result -> assertThat(result.getVariant()).isEqualTo("treatment"))
        .verifyComplete();
  }

  @Test
  void historyListsSnapshots() {
    UUID organizationId = UUID.randomUUID();
    UUID flagId = UUID.randomUUID();
    FeatureFlag flag =
        new FeatureFlag(
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            100,
            "control",
            null,
            null,
            UUID.randomUUID());
    flag.setId(flagId);
    FeatureFlagHistory history =
        new FeatureFlagHistory(
            "UPDATE",
            UUID.randomUUID(),
            Instant.now(),
            flagId,
            organizationId,
            UUID.randomUUID(),
            "PRODUCTION",
            true,
            50,
            "control",
            "{}",
            "{}",
            2L);

    when(flagRepository.findByIdAndOrganization(flagId, organizationId))
        .thenReturn(Mono.just(flag));
    when(historyRepository.listByOrganizationAndFlag(organizationId, flagId))
        .thenReturn(Flux.just(history));

    StepVerifier.create(flagService.history(flagId, organizationId))
        .assertNext(result -> assertThat(result.getHistoryAction()).isEqualTo("UPDATE"))
        .verifyComplete();
  }
}
