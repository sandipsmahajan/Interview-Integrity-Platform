package com.interviewintegrity.featureflag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.exception.ValidationFailedException;
import com.interviewintegrity.featureflag.domain.Experiment;
import com.interviewintegrity.featureflag.domain.ExperimentStatus;
import com.interviewintegrity.featureflag.domain.Feature;
import com.interviewintegrity.featureflag.domain.FlagKind;
import com.interviewintegrity.featureflag.repository.ExperimentRepository;
import com.interviewintegrity.featureflag.repository.FeatureRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the experiment service. */
@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

  @Mock private ExperimentRepository experimentRepository;
  @Mock private FeatureRepository featureRepository;

  private ExperimentService experimentService;

  @BeforeEach
  void setUp() {
    experimentService = new ExperimentService(experimentRepository, featureRepository);
  }

  private Feature liveFeature(UUID organizationId) {
    Feature feature =
        new Feature(
            organizationId, "signup.v2", "Signup V2", null, FlagKind.BOOLEAN, UUID.randomUUID());
    feature.setId(UUID.randomUUID());
    return feature;
  }

  private Experiment runningExperiment(UUID organizationId) {
    Experiment experiment =
        new Experiment(
            organizationId,
            "Signup flow",
            UUID.randomUUID(),
            "legacy",
            "v2",
            "{}",
            UUID.randomUUID());
    experiment.setId(UUID.randomUUID());
    experiment.start(UUID.randomUUID());
    return experiment;
  }

  @Test
  void createCreatesDraftExperiment() {
    UUID organizationId = UUID.randomUUID();
    Feature feature = liveFeature(organizationId);
    when(featureRepository.findLiveById(feature.getId())).thenReturn(Mono.just(feature));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(
            invocation -> {
              Experiment experiment = invocation.getArgument(0);
              experiment.setId(UUID.randomUUID());
              return Mono.just(experiment);
            });

    StepVerifier.create(
            experimentService.create(
                organizationId,
                "Signup flow",
                feature.getId(),
                "legacy",
                "v2",
                "{}",
                UUID.randomUUID()))
        .assertNext(
            experiment -> {
              assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.DRAFT);
              assertThat(experiment.getControlVariant()).isEqualTo("legacy");
            })
        .verifyComplete();
  }

  @Test
  void createRejectsForeignFeature() {
    UUID organizationId = UUID.randomUUID();
    UUID foreignOrganizationId = UUID.randomUUID();
    Feature feature = liveFeature(foreignOrganizationId);
    when(featureRepository.findLiveById(feature.getId())).thenReturn(Mono.just(feature));

    StepVerifier.create(
            experimentService.create(
                organizationId,
                "Signup flow",
                feature.getId(),
                "legacy",
                "v2",
                "{}",
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void createReturnsNotFoundForUnknownFeature() {
    UUID featureId = UUID.randomUUID();
    when(featureRepository.findLiveById(featureId)).thenReturn(Mono.empty());

    StepVerifier.create(
            experimentService.create(
                UUID.randomUUID(),
                "Signup flow",
                featureId,
                "legacy",
                "v2",
                "{}",
                UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void listWithoutStatusListsAll() {
    UUID organizationId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    when(experimentRepository.listByOrganization(organizationId)).thenReturn(Flux.just(experiment));

    StepVerifier.create(experimentService.list(organizationId, null))
        .assertNext(result -> assertThat(result.getName()).isEqualTo("Signup flow"))
        .verifyComplete();
  }

  @Test
  void listWithStatusFiltersByStatus() {
    UUID organizationId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    when(experimentRepository.listByOrganizationAndStatus(organizationId, ExperimentStatus.RUNNING))
        .thenReturn(Flux.just(experiment));

    StepVerifier.create(experimentService.list(organizationId, ExperimentStatus.RUNNING))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.RUNNING))
        .verifyComplete();
  }

  @Test
  void getReturnsExperiment() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));

    StepVerifier.create(experimentService.get(experimentId, organizationId))
        .assertNext(result -> assertThat(result.getId()).isEqualTo(experimentId))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID experimentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(experimentService.get(experimentId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void startTransitionsToRunning() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment =
        new Experiment(
            organizationId,
            "Signup flow",
            UUID.randomUUID(),
            "legacy",
            "v2",
            "{}",
            UUID.randomUUID());
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(experimentService.start(experimentId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.RUNNING))
        .verifyComplete();
  }

  @Test
  void pauseTransitionsToPaused() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(experimentService.pause(experimentId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.PAUSED))
        .verifyComplete();
  }

  @Test
  void resumeTransitionsToRunning() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    experiment.pause(UUID.randomUUID());
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(experimentService.resume(experimentId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.RUNNING))
        .verifyComplete();
  }

  @Test
  void completeTransitionsToCompleted() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment = runningExperiment(organizationId);
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(experimentService.complete(experimentId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.COMPLETED))
        .verifyComplete();
  }

  @Test
  void rejectMarksExperimentRejected() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment =
        new Experiment(
            organizationId,
            "Signup flow",
            UUID.randomUUID(),
            "legacy",
            "v2",
            "{}",
            UUID.randomUUID());
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));
    when(experimentRepository.save(any(Experiment.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(experimentService.reject(experimentId, organizationId, UUID.randomUUID()))
        .assertNext(result -> assertThat(result.getStatus()).isEqualTo(ExperimentStatus.REJECTED))
        .verifyComplete();
  }

  @Test
  void invalidTransitionRejected() {
    UUID organizationId = UUID.randomUUID();
    UUID experimentId = UUID.randomUUID();
    Experiment experiment =
        new Experiment(
            organizationId,
            "Signup flow",
            UUID.randomUUID(),
            "legacy",
            "v2",
            "{}",
            UUID.randomUUID());
    experiment.setId(experimentId);

    when(experimentRepository.findByIdAndOrganization(experimentId, organizationId))
        .thenReturn(Mono.just(experiment));

    StepVerifier.create(experimentService.resume(experimentId, organizationId, UUID.randomUUID()))
        .expectError(ValidationFailedException.class)
        .verify();
  }
}
