package com.integrity.identity.service;

import com.integrity.identity.repository.UserSessionRepository;
import com.integrity.identity.web.dto.SessionResponse;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Session management for the current user: listing and revocation. */
public final class SessionService {

  private final UserSessionRepository sessionRepository;
  private final IdentityMapper mapper;

  /** Creates the session service bound to the session repository. */
  public SessionService(UserSessionRepository sessionRepository, IdentityMapper mapper) {
    this.sessionRepository = sessionRepository;
    this.mapper = mapper;
  }

  /** Lists the sessions of a user, newest first. */
  public Flux<SessionResponse> listUserSessions(UUID userId) {
    return sessionRepository.listByUser(userId, 100, 0).map(mapper::toResponse);
  }

  /** Revokes a single session of a user. */
  public Mono<Void> revokeSession(UUID userId, UUID sessionId) {
    return sessionRepository
        .findById(sessionId)
        .flatMap(
            session -> {
              if (!session.getUserId().equals(userId)) {
                return Mono.empty();
              }
              session.revoke(userId);
              return sessionRepository.save(session).then();
            });
  }

  /** Revokes all active sessions of a user. */
  public Mono<Void> revokeAllSessions(UUID userId) {
    return sessionRepository.revokeAllActiveByUser(userId, java.time.Instant.now()).then();
  }
}
