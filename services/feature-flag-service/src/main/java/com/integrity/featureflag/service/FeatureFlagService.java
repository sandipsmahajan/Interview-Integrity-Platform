package com.integrity.featureflag.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.featureflag.domain.Feature;
import com.integrity.featureflag.domain.FeatureFlag;
import com.integrity.featureflag.domain.FeatureFlagHistory;
import com.integrity.featureflag.domain.FlagTarget;
import com.integrity.featureflag.repository.FeatureFlagHistoryRepository;
import com.integrity.featureflag.repository.FeatureFlagRepository;
import com.integrity.featureflag.repository.FeatureRepository;
import com.integrity.featureflag.repository.FlagTargetRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages feature flag configurations and per-user overrides. */
public class FeatureFlagService {

  private final FeatureRepository featureRepository;
  private final FeatureFlagRepository flagRepository;
  private final FlagTargetRepository targetRepository;
  private final FeatureFlagHistoryRepository historyRepository;

  /** Wires the service with its repositories. */
  public FeatureFlagService(
      FeatureRepository featureRepository,
      FeatureFlagRepository flagRepository,
      FlagTargetRepository targetRepository,
      FeatureFlagHistoryRepository historyRepository) {
    this.featureRepository = featureRepository;
    this.flagRepository = flagRepository;
    this.targetRepository = targetRepository;
    this.historyRepository = historyRepository;
  }

  /** Creates a flag for a feature, rejecting duplicates for the environment. */
  @Transactional
  public Mono<FeatureFlag> createFlag(
      UUID organizationId,
      UUID featureId,
      String environment,
      boolean enabled,
      int rolloutPercent,
      String defaultVariant,
      String variants,
      String rules,
      UUID createdBy) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException("Feature not found")))
        .flatMap(feature -> assertFeatureOrganization(feature, organizationId))
        .then(
            Mono.defer(
                () ->
                    flagRepository
                        .existsByFeatureIdAndEnvironment(featureId, environment)
                        .flatMap(
                            exists -> {
                              if (exists) {
                                return Mono.error(
                                    new ConflictException(
                                        "Flag already exists for the environment"));
                              }
                              return flagRepository.save(
                                  new FeatureFlag(
                                      organizationId,
                                      featureId,
                                      environment,
                                      enabled,
                                      rolloutPercent,
                                      defaultVariant,
                                      variants,
                                      rules,
                                      createdBy));
                            })));
  }

  /** Lists the flags of a feature. */
  @Transactional(readOnly = true)
  public Flux<FeatureFlag> listFlags(UUID organizationId, UUID featureId) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException("Feature not found")))
        .flatMap(feature -> assertFeatureOrganization(feature, organizationId))
        .thenMany(flagRepository.listByOrganizationAndFeature(organizationId, featureId));
  }

  /** Returns a single flag of an organization. */
  @Transactional(readOnly = true)
  public Mono<FeatureFlag> getFlag(UUID flagId, UUID organizationId) {
    return flagRepository
        .findByIdAndOrganization(flagId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Flag not found")));
  }

  /** Updates the rollout configuration of a flag. */
  @Transactional
  public Mono<FeatureFlag> updateFlag(
      UUID flagId,
      UUID organizationId,
      boolean enabled,
      int rolloutPercent,
      String defaultVariant,
      String variants,
      String rules,
      UUID byUser) {
    return flagRepository
        .findByIdAndOrganization(flagId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Flag not found")))
        .map(
            flag -> {
              flag.updateConfiguration(
                  enabled, rolloutPercent, defaultVariant, variants, rules, byUser);
              return flag;
            })
        .flatMap(flagRepository::save);
  }

  /** Adds or replaces a per-user override for a flag. */
  @Transactional
  public Mono<FlagTarget> addTarget(
      UUID flagId, UUID organizationId, UUID userId, String variant, boolean enabled, UUID byUser) {
    return getFlag(flagId, organizationId)
        .then(targetRepository.upsert(flagId, userId, variant, enabled, byUser))
        .thenReturn(new FlagTarget(flagId, userId, variant, enabled, byUser));
  }

  /** Removes a per-user override for a flag. */
  @Transactional
  public Mono<Void> removeTarget(UUID flagId, UUID organizationId, UUID userId) {
    return getFlag(flagId, organizationId).then(targetRepository.remove(flagId, userId));
  }

  /** Lists the per-user overrides of a flag. */
  @Transactional(readOnly = true)
  public Flux<FlagTarget> listTargets(UUID flagId, UUID organizationId) {
    return getFlag(flagId, organizationId).thenMany(targetRepository.listByFlag(flagId));
  }

  /** Lists the history snapshots of a flag. */
  @Transactional(readOnly = true)
  public Flux<FeatureFlagHistory> history(UUID flagId, UUID organizationId) {
    return getFlag(flagId, organizationId)
        .thenMany(historyRepository.listByOrganizationAndFlag(organizationId, flagId));
  }

  private Mono<Feature> assertFeatureOrganization(Feature feature, UUID organizationId) {
    if (!organizationId.equals(feature.getOrganizationId())) {
      return Mono.error(new NotFoundException("Feature not found"));
    }
    return Mono.just(feature);
  }
}
