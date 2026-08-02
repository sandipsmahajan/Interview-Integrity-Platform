package com.interviewintegrity.notification.web;

import com.interviewintegrity.notification.domain.NotificationChannel;
import com.interviewintegrity.notification.service.NotificationMapper;
import com.interviewintegrity.notification.service.NotificationPreferenceService;
import com.interviewintegrity.notification.web.dto.NotificationPreferenceResponse;
import com.interviewintegrity.notification.web.dto.SetPreferenceRequest;
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

/** Notification preference endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@Tag(name = "Notification Preferences", description = "Manage user notification preferences")
public final class NotificationPreferenceController {

  private final NotificationPreferenceService preferenceService;
  private final NotificationMapper mapper;

  /** Creates the controller bound to the preference service and mapper. */
  public NotificationPreferenceController(
      NotificationPreferenceService preferenceService, NotificationMapper mapper) {
    this.preferenceService = preferenceService;
    this.mapper = mapper;
  }

  /** Creates a preference for the given user. */
  @PostMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Set a notification preference")
  public Mono<NotificationPreferenceResponse> set(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody SetPreferenceRequest request) {
    return preferenceService
        .setPreference(
            SecurityPrincipals.organizationId(authentication),
            userId,
            request.channel(),
            request.notificationType().trim(),
            request.enabled())
        .map(mapper::toResponse);
  }

  /** Returns the preference of a user for a channel and type. */
  @GetMapping("/users/{userId}")
  @Operation(summary = "Get a notification preference")
  public Mono<NotificationPreferenceResponse> get(
      Authentication authentication,
      @PathVariable UUID userId,
      @RequestParam NotificationChannel channel,
      @RequestParam String notificationType) {
    return preferenceService
        .getPreference(userId, channel, notificationType)
        .map(mapper::toResponse);
  }

  /** Lists the preferences of a user. */
  @GetMapping("/users/{userId}/list")
  @Operation(summary = "List notification preferences")
  public Flux<NotificationPreferenceResponse> list(
      Authentication authentication, @PathVariable UUID userId) {
    return preferenceService
        .listPreferences(userId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a preference (PUT alias of set). */
  @PutMapping("/users/{userId}")
  @Operation(summary = "Update a notification preference")
  public Mono<NotificationPreferenceResponse> update(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody SetPreferenceRequest request) {
    return preferenceService
        .setPreference(
            SecurityPrincipals.organizationId(authentication),
            userId,
            request.channel(),
            request.notificationType().trim(),
            request.enabled())
        .map(mapper::toResponse);
  }
}
