package com.integrity.notification.service;

import reactor.core.publisher.Mono;

/**
 * Delivers an email message through a concrete provider.
 *
 * <p>Implementations are expected to return a provider message id on success and to propagate the
 * error when dispatch fails so the caller can schedule a retry.
 */
public interface EmailDispatcher {

  /**
   * Sends a multipart email with both HTML and plaintext alternatives.
   *
   * @param to the recipient address
   * @param subject the message subject
   * @param htmlBody the HTML body
   * @param plainText the plaintext fallback body
   * @return the provider message id on success
   */
  Mono<String> send(String to, String subject, String htmlBody, String plainText);
}
