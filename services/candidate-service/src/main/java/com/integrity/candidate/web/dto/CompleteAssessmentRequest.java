package com.integrity.candidate.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * Request to complete an assessment.
 *
 * @param score optional score between 0 and 100
 */
public record CompleteAssessmentRequest(@DecimalMin("0.0") @DecimalMax("100.0") BigDecimal score) {}
