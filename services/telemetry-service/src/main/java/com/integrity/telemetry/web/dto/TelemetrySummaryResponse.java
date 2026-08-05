package com.integrity.telemetry.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public view of an hourly session rollup.
 *
 * @param bucketStart start of the aggregation window
 * @param bucketEnd end of the aggregation window
 * @param sessionId session identifier
 * @param eventType aggregated event type
 * @param eventCount number of events in the window
 * @param minSeq lowest sequence number
 * @param maxSeq highest sequence number
 * @param lastPayload payload of the most recent event
 */
public record TelemetrySummaryResponse(
    Instant bucketStart,
    Instant bucketEnd,
    UUID sessionId,
    String eventType,
    long eventCount,
    Long minSeq,
    Long maxSeq,
    String lastPayload) {}
