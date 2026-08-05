package com.integrity.identity.service;

import com.integrity.event.IdentityEmailEvent;
import reactor.core.publisher.Mono;

/** Publishes email delivery requests onto the platform event bus. */
public interface EmailEventPublisher {

  /**
   * Requests the notification service to deliver an email to a user.
   *
   * @param event the email request describing recipient, template and data
   * @return completion signal of the publish attempt
   */
  Mono<Void> publish(IdentityEmailEvent event);
}
