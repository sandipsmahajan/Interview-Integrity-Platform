package com.interviewintegrity.integration.service;

import com.interviewintegrity.integration.domain.Integration;
import com.interviewintegrity.integration.domain.IntegrationConnection;
import com.interviewintegrity.integration.domain.IntegrationSyncLog;
import com.interviewintegrity.integration.domain.IntegrationWebhook;
import com.interviewintegrity.integration.web.dto.ConnectionResponse;
import com.interviewintegrity.integration.web.dto.IntegrationResponse;
import com.interviewintegrity.integration.web.dto.SyncLogResponse;
import com.interviewintegrity.integration.web.dto.WebhookResponse;
import org.mapstruct.Mapper;

/**
 * Maps integration-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface IntegrationMapper {

  /** Maps an integration into its public response. */
  IntegrationResponse toResponse(Integration integration);

  /** Maps an integration connection into its public response. */
  ConnectionResponse toResponse(IntegrationConnection connection);

  /** Maps an integration webhook into its public response. */
  WebhookResponse toResponse(IntegrationWebhook webhook);

  /** Maps an integration sync log into its public response. */
  SyncLogResponse toResponse(IntegrationSyncLog syncLog);
}
