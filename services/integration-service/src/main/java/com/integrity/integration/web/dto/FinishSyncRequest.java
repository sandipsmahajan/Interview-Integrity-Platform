package com.integrity.integration.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to finish a synchronization run.
 *
 * @param recordsProcessed number of records handled
 * @param errorMessage failure detail
 */
public record FinishSyncRequest(long recordsProcessed, @Size(max = 2000) String errorMessage) {}
