package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.UserSession;
import com.interviewintegrity.identity.repository.UserSessionRepository;
import com.interviewintegrity.identity.web.dto.SessionResponse;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Session management for the current user: listing and revocation. */
public final class SessionService {

  private final UserSessionRepository sessionRepository;

  /** Creates the session service bound to the session repository. */
  public SessionService(UserSessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /** Lists the sessions of a user, newest first. */
  public Flux<SessionResponse> listUserSessions(UUID userId) {
    return sessionRepository.listByUser(userId, 100, 0).map(SessionService::toResponse);
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

  private static SessionResponse toResponse(UserSession session) {
    return new SessionResponse(
        session.getId(),
        session.getDeviceId(),
        session.getIpAddress(),
        session.getUserAgent(),
        session.getStatus().name(),
        session.getIssuedAt(),
        session.getExpiresAt(),
        session.getLastUsedAt());
  }
}
