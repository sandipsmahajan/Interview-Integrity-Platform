package com.interviewintegrity.configuration.service;

import com.interviewintegrity.configuration.domain.Configuration;
import com.interviewintegrity.configuration.domain.ConfigurationHistory;
import com.interviewintegrity.configuration.domain.ConfigurationSchema;
import com.interviewintegrity.configuration.web.dto.ConfigurationHistoryResponse;
import com.interviewintegrity.configuration.web.dto.ConfigurationResponse;
import com.interviewintegrity.configuration.web.dto.ConfigurationSchemaResponse;
import org.mapstruct.Mapper;

/**
 * Maps configuration-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface ConfigurationMapper {

  /** Maps a configuration entry into its public response. */
  ConfigurationResponse toResponse(Configuration configuration);

  /** Maps a configuration history entry into its public response. */
  ConfigurationHistoryResponse toHistoryResponse(ConfigurationHistory history);

  /** Maps a configuration schema into its public response. */
  ConfigurationSchemaResponse toResponse(ConfigurationSchema schema);
}
