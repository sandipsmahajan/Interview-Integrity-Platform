package com.interviewintegrity.integration.web;

import com.interviewintegrity.integration.domain.IntegrationWebhook;
import com.interviewintegrity.integration.service.IntegrationWebhookService;
import com.interviewintegrity.integration.web.dto.CreateWebhookRequest;
import com.interviewintegrity.integration.web.dto.UpdateWebhookRequest;
import com.interviewintegrity.integration.web.dto.WebhookResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Webhook endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/integration-webhooks")
@Tag(name = "Integration Webhooks", description = "Manage outbound webhook subscriptions")
public final class IntegrationWebhookController {

  private final IntegrationWebhookService webhookService;

  /** Creates the controller bound to the webhook service. */
  public IntegrationWebhookController(IntegrationWebhookService webhookService) {
    this.webhookService = webhookService;
  }

  /** Creates a webhook subscription. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an integration webhook")
  public Mono<WebhookResponse> create(
      Authentication authentication, @Valid @RequestBody CreateWebhookRequest request) {
    return webhookService
        .createWebhook(
            SecurityPrincipals.organizationId(authentication),
            request.integrationId(),
            request.url().trim(),
            request.secretHash(),
            request.events())
        .map(this::toResponse);
  }

  /** Lists the webhooks of an integration. */
  @GetMapping
  @Operation(summary = "List integration webhooks")
  public Flux<WebhookResponse> list(
      Authentication authentication, @RequestParam UUID integrationId) {
    return webhookService
        .listByIntegration(integrationId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Returns a single webhook. */
  @GetMapping("/{webhookId}")
  @Operation(summary = "Get an integration webhook")
  public Mono<WebhookResponse> get(Authentication authentication, @PathVariable UUID webhookId) {
    return webhookService
        .getWebhook(webhookId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Updates a webhook subscription. */
  @PutMapping("/{webhookId}")
  @Operation(summary = "Update an integration webhook")
  public Mono<WebhookResponse> update(
      Authentication authentication,
      @PathVariable UUID webhookId,
      @Valid @RequestBody UpdateWebhookRequest request) {
    return webhookService
        .updateWebhook(
            webhookId,
            SecurityPrincipals.organizationId(authentication),
            request.url().trim(),
            request.events())
        .map(this::toResponse);
  }

  /** Enables a webhook. */
  @PostMapping("/{webhookId}/enable")
  @Operation(summary = "Enable an integration webhook")
  public Mono<WebhookResponse> enable(Authentication authentication, @PathVariable UUID webhookId) {
    return webhookService
        .enableWebhook(webhookId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Disables a webhook. */
  @PostMapping("/{webhookId}/disable")
  @Operation(summary = "Disable an integration webhook")
  public Mono<WebhookResponse> disable(
      Authentication authentication, @PathVariable UUID webhookId) {
    return webhookService
        .disableWebhook(webhookId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  private WebhookResponse toResponse(IntegrationWebhook webhook) {
    return new WebhookResponse(
        webhook.getId(),
        webhook.getOrganizationId(),
        webhook.getIntegrationId(),
        webhook.getUrl(),
        webhook.getEvents(),
        webhook.isEnabled(),
        webhook.getCreatedAt(),
        webhook.getUpdatedAt());
  }
}
