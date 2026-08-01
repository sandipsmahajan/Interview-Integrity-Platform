package com.interviewintegrity.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A device trusted to skip MFA challenges.
 *
 * @param id trusted device record identifier
 * @param deviceId stable device identifier
 * @param deviceName human readable device label
 * @param lastSeenAt instant the device last authenticated
 */
public record TrustedDeviceResponse(
    UUID id, String deviceId, String deviceName, Instant lastSeenAt) {}
