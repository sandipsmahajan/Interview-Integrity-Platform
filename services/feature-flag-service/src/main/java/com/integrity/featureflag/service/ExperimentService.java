package com.integrity.featureflag.service;

import com.integrity.exception.NotFoundException;
import com.integrity.featureflag.domain.Experiment;
import com.integrity.featureflag.domain.ExperimentStatus;
import com.integrity.featureflag.domain.Feature;
import com.integrity.featureflag.repository.ExperimentRepository;
import com.integrity.featureflag.repository.FeatureRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages A/B experiments targeting feature flags. */
public class ExperimentService {

  private final ExperimentRepository experimentRepository;
  private final FeatureRepository featureRepository;

  /** Wires the service with its repositories. */
  public ExperimentService(
      ExperimentRepository experimentRepository, FeatureRepository featureRepository) {
    this.experimentRepository = experimentRepository;
    this.featureRepository = featureRepository;
  }

  /** Creates a draft experiment for a feature. */
  @Transactional
  public Mono<Experiment> create(
      UUID organizationId,
      String name,
      UUID featureId,
      String controlVariant,
      String treatmentVariant,
      String metrics,
      UUID createdBy) {
    return featureRepository
        .findLiveById(featureId)
        .switchIfEmpty(Mono.error(new NotFoundException("Feature not found")))
        .flatMap(feature -> assertFeatureOrganization(feature, organizationId))
        .then(
            Mono.defer(
                () ->
                    experimentRepository.save(
                        new Experiment(
                            organizationId,
                            name,
                            featureId,
                            controlVariant,
                            treatmentVariant,
                            metrics,
                            createdBy))));
  }

  /** Lists the experiments of an organization, optionally filtered by status. */
  @Transactional(readOnly = true)
  public Flux<Experiment> list(UUID organizationId, ExperimentStatus status) {
    if (status == null) {
      return experimentRepository.listByOrganization(organizationId);
    }
    return experimentRepository.listByOrganizationAndStatus(organizationId, status);
  }

  /** Returns a single experiment of an organization. */
  @Transactional(readOnly = true)
  public Mono<Experiment> get(UUID experimentId, UUID organizationId) {
    return experimentRepository
        .findByIdAndOrganization(experimentId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Experiment not found")));
  }

  /** Starts the experiment. */
  @Transactional
  public Mono<Experiment> start(UUID experimentId, UUID organizationId, UUID byUser) {
    return transition(experimentId, organizationId, byUser, TransitionAction.START);
  }

  /** Pauses the experiment. */
  @Transactional
  public Mono<Experiment> pause(UUID experimentId, UUID organizationId, UUID byUser) {
    return transition(experimentId, organizationId, byUser, TransitionAction.PAUSE);
  }

  /** Resumes the experiment. */
  @Transactional
  public Mono<Experiment> resume(UUID experimentId, UUID organizationId, UUID byUser) {
    return transition(experimentId, organizationId, byUser, TransitionAction.RESUME);
  }

  /** Completes the experiment. */
  @Transactional
  public Mono<Experiment> complete(UUID experimentId, UUID organizationId, UUID byUser) {
    return transition(experimentId, organizationId, byUser, TransitionAction.COMPLETE);
  }

  /** Rejects the experiment. */
  @Transactional
  public Mono<Experiment> reject(UUID experimentId, UUID organizationId, UUID byUser) {
    return transition(experimentId, organizationId, byUser, TransitionAction.REJECT);
  }

  private Mono<Experiment> transition(
      UUID experimentId, UUID organizationId, UUID byUser, TransitionAction action) {
    return experimentRepository
        .findByIdAndOrganization(experimentId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Experiment not found")))
        .map(
            experiment -> {
              switch (action) {
                case START -> experiment.start(byUser);
                case PAUSE -> experiment.pause(byUser);
                case RESUME -> experiment.resume(byUser);
                case COMPLETE -> experiment.complete(byUser);
                case REJECT -> experiment.reject(byUser);
              }
              return experiment;
            })
        .flatMap(experimentRepository::save);
  }

  private Mono<Feature> assertFeatureOrganization(Feature feature, UUID organizationId) {
    if (!organizationId.equals(feature.getOrganizationId())) {
      return Mono.error(new NotFoundException("Feature not found"));
    }
    return Mono.just(feature);
  }

  private enum TransitionAction {
    START,
    PAUSE,
    RESUME,
    COMPLETE,
    REJECT
  }
}
