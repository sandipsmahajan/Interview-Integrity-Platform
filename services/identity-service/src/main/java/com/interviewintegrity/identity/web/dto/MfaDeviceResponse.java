package com.interviewintegrity.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * MFA device visible to the owning user.
 *
 * @param id device identifier
 * @param kind device kind, e.g. {@code TOTP}
 * @param verifiedAt instant the device was activated
 * @param lastUsedAt instant the device last verified a challenge
 */
public record MfaDeviceResponse(UUID id, String kind, Instant verifiedAt, Instant lastUsedAt) {}
