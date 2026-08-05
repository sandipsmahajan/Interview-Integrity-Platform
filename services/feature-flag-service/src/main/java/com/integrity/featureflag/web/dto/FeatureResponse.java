package com.integrity.featureflag.web.dto;

import com.integrity.featureflag.domain.FlagKind;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a feature.
 *
 * @param id feature identifier
 * @param organizationId owning tenant
 * @param code stable machine readable code
 * @param name display name
 * @param description human readable description
 * @param kind value kind of the feature flag
 * @param createdAt instant the feature was created
 */
public record FeatureResponse(
    UUID id,
    UUID organizationId,
    String code,
    String name,
    String description,
    FlagKind kind,
    Instant createdAt) {}
