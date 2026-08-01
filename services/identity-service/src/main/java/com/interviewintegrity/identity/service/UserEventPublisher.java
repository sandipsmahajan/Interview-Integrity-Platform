package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.User;
import reactor.core.publisher.Mono;

/** Publishes identity domain events onto the platform event bus. */
public interface UserEventPublisher {

  /**
   * Publishes the user registration event.
   *
   * @param user the registered user
   * @return completion signal of the publish attempt
   */
  Mono<Void> publishUserRegistered(User user);
}
