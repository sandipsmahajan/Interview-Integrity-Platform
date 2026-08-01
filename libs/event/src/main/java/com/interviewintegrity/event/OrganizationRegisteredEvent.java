package com.interviewintegrity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an organization (tenant) is registered on the platform.
 *
 * @param organizationId registered tenant identifier
 * @param name organization display name
 * @param slug unique URL slug of the organization
 * @param occurredAt instant of registration
 */
public record OrganizationRegisteredEvent(
    UUID organizationId, String name, String slug, Instant occurredAt) {}
