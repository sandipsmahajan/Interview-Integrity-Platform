package com.interviewintegrity.storage.web.dto;

import com.interviewintegrity.storage.domain.UrlPurpose;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a pre-signed URL grant.
 *
 * @param id grant identifier
 * @param organizationId owning tenant
 * @param objectId owning object
 * @param purpose operation the grant authorizes
 * @param token raw token issued to the caller, only visible at creation
 * @param expiresAt instant the grant becomes invalid
 * @param maxUses maximum allowed uses, unlimited when null
 * @param usageCount number of uses recorded
 * @param createdAt instant the grant was created
 * @param revokedAt instant the grant was revoked
 */
public record SignedUrlResponse(
    UUID id,
    UUID organizationId,
    UUID objectId,
    UrlPurpose purpose,
    String token,
    Instant expiresAt,
    Integer maxUses,
    int usageCount,
    Instant createdAt,
    Instant revokedAt) {}
