package com.integrity.organization.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Registered billing address of an organization.
 *
 * @param id address identifier
 * @param line1 first address line
 * @param line2 second address line
 * @param city city
 * @param region region or state
 * @param postalCode postal code
 * @param countryCode two letter country code
 * @param updatedAt instant of the last update
 */
public record AddressResponse(
    UUID id,
    String line1,
    String line2,
    String city,
    String region,
    String postalCode,
    String countryCode,
    Instant updatedAt) {}
