package com.integrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Claimed email domain of an organization.
 *
 * @param id domain claim identifier
 * @param organizationId owning tenant
 * @param domain normalized domain name
 * @param verifiedAt instant the domain was verified, null when unverified
 * @param createdAt instant the claim was created
 */
public record DomainResponse(
    UUID id, UUID organizationId, String domain, Instant verifiedAt, Instant createdAt) {}
