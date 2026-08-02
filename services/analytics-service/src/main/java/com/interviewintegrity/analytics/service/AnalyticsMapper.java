package com.interviewintegrity.analytics.service;

import com.interviewintegrity.analytics.domain.AnalyticsJobRun;
import com.interviewintegrity.analytics.web.dto.JobRunResponse;
import org.mapstruct.Mapper;

/**
 * Maps analytics-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface AnalyticsMapper {

  /** Maps an analytics job run into its public response. */
  JobRunResponse toResponse(AnalyticsJobRun run);
}
