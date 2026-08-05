package com.integrity.storage.web.dto;

import com.integrity.storage.domain.UrlPurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Request to issue a pre-signed URL grant.
 *
 * @param purpose operation the grant authorizes
 * @param expiresAt instant the grant becomes invalid
 * @param maxUses maximum allowed uses, unlimited when null
 */
public record CreateSignedUrlRequest(
    @NotNull UrlPurpose purpose, @NotNull Instant expiresAt, @Min(1) Integer maxUses) {}
