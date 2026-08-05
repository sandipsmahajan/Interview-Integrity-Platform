package com.integrity.identity.web;

import com.integrity.identity.service.SessionService;
import com.integrity.identity.web.dto.SessionResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Endpoints to inspect and revoke the caller's own sessions. */
@RestController
@RequestMapping("/api/v1/auth/sessions")
@Tag(name = "Sessions", description = "Manage the caller's active sessions")
public final class SessionController {

  private final SessionService sessionService;

  /** Creates the controller bound to the session service. */
  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  /** Lists the sessions of the caller. */
  @GetMapping
  @Operation(summary = "List my sessions")
  public Flux<SessionResponse> listMySessions(Authentication authentication) {
    return sessionService.listUserSessions(SecurityPrincipals.userId(authentication));
  }

  /** Revokes one of the caller's sessions. */
  @DeleteMapping("/{sessionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke a session")
  public Mono<Void> revokeSession(Authentication authentication, @PathVariable UUID sessionId) {
    return sessionService.revokeSession(SecurityPrincipals.userId(authentication), sessionId);
  }

  /** Revokes all of the caller's sessions. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Revoke all sessions")
  public Mono<Void> revokeAll(Authentication authentication) {
    return sessionService.revokeAllSessions(SecurityPrincipals.userId(authentication));
  }
}
