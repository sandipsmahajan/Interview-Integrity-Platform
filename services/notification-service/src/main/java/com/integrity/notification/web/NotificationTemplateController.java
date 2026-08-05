package com.integrity.notification.web;

import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.service.NotificationMapper;
import com.integrity.notification.service.NotificationTemplateService;
import com.integrity.notification.web.dto.CreateNotificationTemplateRequest;
import com.integrity.notification.web.dto.NotificationTemplateResponse;
import com.integrity.notification.web.dto.UpdateNotificationTemplateRequest;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Notification template endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/notification-templates")
@Tag(name = "Notification Templates", description = "Manage message templates")
public final class NotificationTemplateController {

  private final NotificationTemplateService templateService;
  private final NotificationMapper mapper;

  /** Creates the controller bound to the template service and mapper. */
  public NotificationTemplateController(
      NotificationTemplateService templateService, NotificationMapper mapper) {
    this.templateService = templateService;
    this.mapper = mapper;
  }

  /** Creates a template. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a notification template")
  public Mono<NotificationTemplateResponse> create(
      Authentication authentication,
      @Valid @RequestBody CreateNotificationTemplateRequest request) {
    return templateService
        .createTemplate(
            SecurityPrincipals.organizationId(authentication),
            request.code().trim(),
            request.channel(),
            request.subject(),
            request.bodyTemplate(),
            request.locale())
        .map(mapper::toResponse);
  }

  /** Lists the templates of the organization, optionally filtered by channel. */
  @GetMapping
  @Operation(summary = "List notification templates")
  public Flux<NotificationTemplateResponse> list(
      Authentication authentication, @RequestParam(required = false) NotificationChannel channel) {
    return templateService
        .listTemplates(SecurityPrincipals.organizationId(authentication))
        .filter(template -> channel == null || template.getChannel() == channel)
        .map(mapper::toResponse);
  }

  /** Returns a single template. */
  @GetMapping("/{templateId}")
  @Operation(summary = "Get a notification template")
  public Mono<NotificationTemplateResponse> get(
      Authentication authentication, @PathVariable UUID templateId) {
    return templateService
        .getTemplate(templateId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a template. */
  @PutMapping("/{templateId}")
  @Operation(summary = "Update a notification template")
  public Mono<NotificationTemplateResponse> update(
      Authentication authentication,
      @PathVariable UUID templateId,
      @Valid @RequestBody UpdateNotificationTemplateRequest request) {
    return templateService
        .updateTemplate(
            templateId,
            SecurityPrincipals.organizationId(authentication),
            request.subject(),
            request.bodyTemplate(),
            request.locale())
        .map(mapper::toResponse);
  }

  /** Marks a template as the tenant default for its code and channel. */
  @PostMapping("/{templateId}/default")
  @Operation(summary = "Set a notification template as default")
  public Mono<NotificationTemplateResponse> setDefault(
      Authentication authentication, @PathVariable UUID templateId) {
    return templateService
        .setDefault(templateId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a template. */
  @DeleteMapping("/{templateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a notification template")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID templateId) {
    return templateService.deleteTemplate(
        templateId, SecurityPrincipals.organizationId(authentication));
  }
}
