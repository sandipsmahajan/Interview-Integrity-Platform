package com.interviewintegrity.integration.web.dto;

import com.interviewintegrity.integration.domain.SyncDirection;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to start a synchronization run.
 *
 * @param connectionId target connection
 * @param direction sync direction
 */
public record StartSyncRequest(@NotNull UUID connectionId, @NotNull SyncDirection direction) {}
