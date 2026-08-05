package com.integrity.analytics.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to complete an analytics job run.
 *
 * @param recordsProcessed number of records processed
 * @param errorMessage failure detail
 */
public record CompleteJobRunRequest(long recordsProcessed, @Size(max = 2000) String errorMessage) {}
