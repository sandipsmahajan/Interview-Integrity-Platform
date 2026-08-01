package com.interviewintegrity.organization.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to update the registered billing address.
 *
 * @param line1 first address line
 * @param line2 second address line
 * @param city city
 * @param region region or state
 * @param postalCode postal code
 * @param countryCode two letter country code
 */
public record UpdateAddressRequest(
    @Size(max = 200) String line1,
    @Size(max = 200) String line2,
    @Size(max = 100) String city,
    @Size(max = 100) String region,
    @Size(max = 20) String postalCode,
    @Size(min = 2, max = 2)
        @Pattern(regexp = "^[A-Z]{2}$", message = "country code must be two uppercase letters")
        String countryCode) {}
