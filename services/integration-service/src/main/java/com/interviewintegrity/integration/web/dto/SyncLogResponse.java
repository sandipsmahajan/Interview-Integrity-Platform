package com.interviewintegrity.integration.web.dto;

import com.interviewintegrity.integration.domain.SyncDirection;
import com.interviewintegrity.integration.domain.SyncStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of a synchronization run.
 *
 * @param id sync log identifier
 * @param organizationId owning tenant
 * @param connectionId target connection
 * @param direction sync direction
 * @param status lifecycle state
 * @param recordsProcessed number of records handled
 * @param errorMessage failure detail
 * @param startedAt start instant
 * @param finishedAt finish instant
 * @param durationMs run duration in milliseconds
 */
public record SyncLogResponse(
    Long id,
    UUID organizationId,
    UUID connectionId,
    SyncDirection direction,
    SyncStatus status,
    long recordsProcessed,
    String errorMessage,
    Instant startedAt,
    Instant finishedAt,
    Long durationMs) {}
