package com.interviewintegrity.integration.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.integration.domain.IntegrationWebhook;
import com.interviewintegrity.integration.repository.IntegrationRepository;
import com.interviewintegrity.integration.repository.IntegrationWebhookRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the outbound webhook subscriptions of an organization. */
public class IntegrationWebhookService {

  private final IntegrationWebhookRepository webhookRepository;
  private final IntegrationRepository integrationRepository;

  /** Wires the service with its repositories. */
  public IntegrationWebhookService(
      IntegrationWebhookRepository webhookRepository, IntegrationRepository integrationRepository) {
    this.webhookRepository = webhookRepository;
    this.integrationRepository = integrationRepository;
  }

  /** Creates a webhook subscription under an integration. */
  @Transactional
  public Mono<IntegrationWebhook> createWebhook(
      UUID organizationId, UUID integrationId, String url, String secretHash, List<String> events) {
    return integrationRepository
        .findLiveByIdAndOrganization(integrationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration not found")))
        .flatMap(
            ignored ->
                webhookRepository.save(
                    new IntegrationWebhook(
                        organizationId, integrationId, url, secretHash, events)));
  }

  /** Returns a single webhook of the organization. */
  @Transactional(readOnly = true)
  public Mono<IntegrationWebhook> getWebhook(UUID webhookId, UUID organizationId) {
    return webhookRepository
        .findByIdAndOrganization(webhookId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration webhook not found")));
  }

  /** Lists the webhooks of an integration. */
  @Transactional(readOnly = true)
  public Flux<IntegrationWebhook> listByIntegration(UUID integrationId, UUID organizationId) {
    return integrationRepository
        .findLiveByIdAndOrganization(integrationId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Integration not found")))
        .flatMapMany(ignored -> webhookRepository.listByIntegration(integrationId, organizationId));
  }

  /** Updates the delivery url and subscribed events of a webhook. */
  @Transactional
  public Mono<IntegrationWebhook> updateWebhook(
      UUID webhookId, UUID organizationId, String url, List<String> events) {
    return getWebhook(webhookId, organizationId)
        .map(
            webhook -> {
              webhook.update(url, events);
              return webhook;
            })
        .flatMap(webhookRepository::save);
  }

  /** Enables a webhook. */
  @Transactional
  public Mono<IntegrationWebhook> enableWebhook(UUID webhookId, UUID organizationId) {
    return getWebhook(webhookId, organizationId)
        .map(
            webhook -> {
              webhook.enable();
              return webhook;
            })
        .flatMap(webhookRepository::save);
  }

  /** Disables a webhook. */
  @Transactional
  public Mono<IntegrationWebhook> disableWebhook(UUID webhookId, UUID organizationId) {
    return getWebhook(webhookId, organizationId)
        .map(
            webhook -> {
              webhook.disable();
              return webhook;
            })
        .flatMap(webhookRepository::save);
  }
}
