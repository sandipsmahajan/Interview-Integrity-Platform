package com.integrity.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a user session.
 *
 * @param id session identifier
 * @param deviceId device identifier, when provided
 * @param ipAddress source address, when captured
 * @param userAgent client user agent, when captured
 * @param status session lifecycle status
 * @param issuedAt creation instant
 * @param expiresAt expiry instant
 * @param lastUsedAt last use instant, null when never used
 */
public record SessionResponse(
    UUID id,
    String deviceId,
    String ipAddress,
    String userAgent,
    String status,
    Instant issuedAt,
    Instant expiresAt,
    Instant lastUsedAt) {}
