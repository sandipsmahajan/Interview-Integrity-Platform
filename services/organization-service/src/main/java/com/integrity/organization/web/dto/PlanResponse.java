package com.integrity.organization.web.dto;

import java.util.UUID;

/**
 * Public profile of a subscription plan.
 *
 * @param id plan identifier
 * @param code plan code
 * @param name display name
 * @param monthlyPriceCents monthly price in cents
 * @param maxSeats maximum seat count, null when unlimited
 * @param features JSON feature blob
 */
public record PlanResponse(
    UUID id, String code, String name, long monthlyPriceCents, Integer maxSeats, String features) {}
