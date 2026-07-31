package com.interviewintegrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user account is registered on the platform.
 *
 * @param userId registered user identifier
 * @param companyId owning company identifier
 * @param email user email address
 * @param occurredAt instant of registration
 */
public record UserRegisteredEvent(UUID userId, UUID companyId, String email, Instant occurredAt) {}
