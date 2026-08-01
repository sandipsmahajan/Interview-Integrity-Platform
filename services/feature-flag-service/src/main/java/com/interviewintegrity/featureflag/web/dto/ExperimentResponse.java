package com.interviewintegrity.featureflag.web.dto;

import com.interviewintegrity.featureflag.domain.ExperimentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of an experiment.
 *
 * @param id experiment identifier
 * @param organizationId owning tenant
 * @param name display name
 * @param featureId targeted feature
 * @param controlVariant control variant
 * @param treatmentVariant treatment variant
 * @param status lifecycle state
 * @param startedAt instant the experiment started
 * @param endedAt instant the experiment ended
 * @param createdAt instant the experiment was created
 */
public record ExperimentResponse(
    UUID id,
    UUID organizationId,
    String name,
    UUID featureId,
    String controlVariant,
    String treatmentVariant,
    ExperimentStatus status,
    Instant startedAt,
    Instant endedAt,
    Instant createdAt) {}
