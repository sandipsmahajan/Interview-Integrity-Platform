package com.integrity.candidate.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload of the candidate registration event.
 *
 * @param candidateId registered candidate identifier
 * @param organizationId owning tenant identifier
 * @param email candidate contact email
 * @param fullName candidate display name
 * @param occurredAt instant of registration
 */
public record CandidateRegisteredEvent(
    UUID candidateId, UUID organizationId, String email, String fullName, Instant occurredAt) {}
