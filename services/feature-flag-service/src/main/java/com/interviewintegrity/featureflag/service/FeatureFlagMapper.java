package com.interviewintegrity.featureflag.service;

import com.interviewintegrity.featureflag.domain.Experiment;
import com.interviewintegrity.featureflag.domain.Feature;
import com.interviewintegrity.featureflag.domain.FeatureFlag;
import com.interviewintegrity.featureflag.domain.FeatureFlagHistory;
import com.interviewintegrity.featureflag.domain.FlagTarget;
import com.interviewintegrity.featureflag.web.dto.ExperimentResponse;
import com.interviewintegrity.featureflag.web.dto.FeatureFlagHistoryResponse;
import com.interviewintegrity.featureflag.web.dto.FeatureFlagResponse;
import com.interviewintegrity.featureflag.web.dto.FeatureResponse;
import com.interviewintegrity.featureflag.web.dto.FlagTargetResponse;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps feature-flag-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time; only the flag history requires an explicit rename because the
 * entity exposes its key as {@code id} while the record names it {@code featureFlagId}.
 */
@Mapper(componentModel = "spring")
public interface FeatureFlagMapper {

  /** Maps a feature into its public response. */
  FeatureResponse toResponse(Feature feature);

  /** Maps a feature flag into its public response. */
  FeatureFlagResponse toResponse(FeatureFlag flag);

  /** Maps a flag target into its public response. */
  FlagTargetResponse toTargetResponse(FlagTarget target);

  /** Maps a flag history entry into its public response. */
  @Mapping(target = "featureFlagId", source = "history", qualifiedByName = "mapFlagId")
  FeatureFlagHistoryResponse toHistoryResponse(FeatureFlagHistory history);

  /** Extracts the stored flag UUID from a history entry. */
  @org.mapstruct.Named("mapFlagId")
  default UUID mapFlagId(FeatureFlagHistory history) {
    return history.getFlagId();
  }

  /** Maps an experiment into its public response. */
  ExperimentResponse toResponse(Experiment experiment);
}
