package com.integrity.telemetry.service;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Publishes detected violation signals onto the platform event bus. */
public interface TelemetryViolationPublisher {

  /**
   * Publishes a violation signal for an organization.
   *
   * @param organizationId owning tenant
   * @param event the violation to publish
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishViolation(UUID organizationId, TelemetryViolationEvent event);
}
