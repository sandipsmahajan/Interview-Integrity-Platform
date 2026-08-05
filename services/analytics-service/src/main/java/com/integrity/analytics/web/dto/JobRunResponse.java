package com.integrity.analytics.web.dto;

import java.time.Instant;

/**
 * Public view of an analytics job run.
 *
 * @param id job run identifier
 * @param jobName aggregation job name
 * @param status run status
 * @param recordsProcessed number of records processed
 * @param errorMessage failure detail
 * @param startedAt start instant
 * @param finishedAt finish instant
 * @param durationMs run duration
 */
public record JobRunResponse(
    Long id,
    String jobName,
    String status,
    long recordsProcessed,
    String errorMessage,
    Instant startedAt,
    Instant finishedAt,
    Long durationMs) {}
