package com.integrity.featureflag.config;

import com.integrity.featureflag.repository.ExperimentRepository;
import com.integrity.featureflag.repository.FeatureFlagHistoryRepository;
import com.integrity.featureflag.repository.FeatureFlagRepository;
import com.integrity.featureflag.repository.FeatureRepository;
import com.integrity.featureflag.repository.FlagTargetRepository;
import com.integrity.featureflag.service.ExperimentService;
import com.integrity.featureflag.service.FeatureFlagService;
import com.integrity.featureflag.service.FeatureService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Explicit bean wiring for the feature flag service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the feature service. */
  @Bean
  public FeatureService featureService(FeatureRepository featureRepository) {
    return new FeatureService(featureRepository);
  }

  /** Provides the feature flag service. */
  @Bean
  public FeatureFlagService featureFlagService(
      FeatureRepository featureRepository,
      FeatureFlagRepository flagRepository,
      FlagTargetRepository targetRepository,
      FeatureFlagHistoryRepository historyRepository) {
    return new FeatureFlagService(
        featureRepository, flagRepository, targetRepository, historyRepository);
  }

  /** Provides the experiment service. */
  @Bean
  public ExperimentService experimentService(
      ExperimentRepository experimentRepository, FeatureRepository featureRepository) {
    return new ExperimentService(experimentRepository, featureRepository);
  }

  /** Provides the composite key flag target repository. */
  @Bean
  public FlagTargetRepository flagTargetRepository(DatabaseClient databaseClient) {
    return new FlagTargetRepository(databaseClient);
  }
}
