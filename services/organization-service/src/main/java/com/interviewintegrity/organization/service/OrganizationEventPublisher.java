package com.interviewintegrity.organization.service;

import com.interviewintegrity.organization.domain.Organization;
import reactor.core.publisher.Mono;

/** Publishes organization domain events onto the platform event bus. */
public interface OrganizationEventPublisher {

  /**
   * Publishes the organization registration event.
   *
   * @param organization the registered organization
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishOrganizationRegistered(Organization organization);
}
