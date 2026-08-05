package com.integrity.integration.service;

import com.integrity.integration.domain.Integration;
import com.integrity.integration.domain.IntegrationConnection;
import com.integrity.integration.domain.IntegrationSyncLog;
import com.integrity.integration.domain.IntegrationWebhook;
import com.integrity.integration.web.dto.ConnectionResponse;
import com.integrity.integration.web.dto.IntegrationResponse;
import com.integrity.integration.web.dto.SyncLogResponse;
import com.integrity.integration.web.dto.WebhookResponse;
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
