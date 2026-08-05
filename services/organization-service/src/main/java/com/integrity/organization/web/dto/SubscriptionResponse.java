package com.integrity.organization.web.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public profile of a tenant subscription.
 *
 * @param id subscription identifier
 * @param organizationId owning tenant
 * @param planCode code of the subscribed plan
 * @param planName name of the subscribed plan
 * @param status billing status
 * @param currentPeriodStart start of the current billing period
 * @param currentPeriodEnd end of the current billing period
 * @param cancelAtPeriodEnd whether cancellation is scheduled at period end
 */
public record SubscriptionResponse(
    UUID id,
    UUID organizationId,
    String planCode,
    String planName,
    String status,
    LocalDate currentPeriodStart,
    LocalDate currentPeriodEnd,
    boolean cancelAtPeriodEnd) {}
