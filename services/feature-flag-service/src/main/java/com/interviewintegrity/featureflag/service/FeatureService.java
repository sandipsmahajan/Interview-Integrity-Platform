package com.interviewintegrity.featureflag.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.featureflag.domain.Feature;
import com.interviewintegrity.featureflag.domain.FlagKind;
import com.interviewintegrity.featureflag.repository.FeatureRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the feature catalog of an organization. */
public class FeatureService {

  private static final String FEATURE_NOT_FOUND = "Feature not found";

  private final FeatureRepository featureRepository;

  /** Wires the service with its repository. */
  public FeatureService(FeatureRepository featureRepository) {
    this.featureRepository = featureRepository;
  }

  /** Creates a feature, rejecting duplicate codes. */
  @Transactional
  public Mono<Feature> create(
      UUID organizationId,
      String code,
      String name,
      String description,
      FlagKind kind,
      UUID createdBy) {
    return featureRepository
        .existsByOrganizationAndCode(organizationId, code)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Feature code already exists"));
              }
              return featureRepository.save(
                  new Feature(organizationId, code, name, description, kind, createdBy));
            });
  }

  /** Lists the features of an organization. */
  @Transactional(readOnly = true)
  public Flux<Feature> list(UUID organizationId) {
    return featureRepository.listLiveByOrganization(organizationId);
  }

  /** Returns a single feature of an organization. */
  @Transactional(readOnly = true)
  public Mono<Feature> get(UUID featureId, UUID organizationId) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException(FEATURE_NOT_FOUND)))
        .flatMap(feature -> assertOrganization(feature, organizationId));
  }

  /** Updates a feature. */
  @Transactional
  public Mono<Feature> update(
      UUID featureId, UUID organizationId, String name, String description, UUID byUser) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException(FEATURE_NOT_FOUND)))
        .flatMap(feature -> assertOrganization(feature, organizationId))
        .map(
            feature -> {
              feature.update(name, description, byUser);
              return feature;
            })
        .flatMap(featureRepository::save);
  }

  /** Soft deletes a feature. */
  @Transactional
  public Mono<Void> delete(UUID featureId, UUID organizationId, UUID byUser) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException(FEATURE_NOT_FOUND)))
        .flatMap(feature -> assertOrganization(feature, organizationId))
        .map(
            feature -> {
              feature.delete(byUser);
              return feature;
            })
        .flatMap(featureRepository::save)
        .then();
  }

  private Mono<Feature> assertOrganization(Feature feature, UUID organizationId) {
    if (!organizationId.equals(feature.getOrganizationId())) {
      return Mono.error(new NotFoundException(FEATURE_NOT_FOUND));
    }
    return Mono.just(feature);
  }
}
