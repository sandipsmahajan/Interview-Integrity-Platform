package com.integrity.featureflag.service;

import com.integrity.featureflag.domain.Experiment;
import com.integrity.featureflag.domain.Feature;
import com.integrity.featureflag.domain.FeatureFlag;
import com.integrity.featureflag.domain.FeatureFlagHistory;
import com.integrity.featureflag.domain.FlagTarget;
import com.integrity.featureflag.web.dto.ExperimentResponse;
import com.integrity.featureflag.web.dto.FeatureFlagHistoryResponse;
import com.integrity.featureflag.web.dto.FeatureFlagResponse;
import com.integrity.featureflag.web.dto.FeatureResponse;
import com.integrity.featureflag.web.dto.FlagTargetResponse;
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
