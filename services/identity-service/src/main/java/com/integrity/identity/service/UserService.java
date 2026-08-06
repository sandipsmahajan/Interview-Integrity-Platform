package com.integrity.identity.service;

import com.integrity.api.PageResponse;
import com.integrity.api.PageResponses;
import com.integrity.event.IdentityEmailEvent;
import com.integrity.exception.ConflictException;
import com.integrity.exception.ForbiddenException;
import com.integrity.exception.NotFoundException;
import com.integrity.identity.domain.User;
import com.integrity.identity.domain.UserStatus;
import com.integrity.identity.repository.RoleRepository;
import com.integrity.identity.repository.UserRepository;
import com.integrity.identity.repository.UserRoleRepository;
import com.integrity.identity.web.dto.AssignRolesRequest;
import com.integrity.identity.web.dto.ChangeUserStatusRequest;
import com.integrity.identity.web.dto.CreateUserRequest;
import com.integrity.identity.web.dto.UpdateUserRequest;
import com.integrity.identity.web.dto.UserResponse;
import com.integrity.security.JwtTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** User management within an organization: creation, lookup, updates, status and roles. */
public final class UserService {

  private static final Duration INVITATION_TOKEN_TTL = Duration.ofDays(7);
  private static final String DEFAULT_LOCALE = "en";
  private static final String PURPOSE_INVITATION = "invitation";

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final UserResponseMapper responseMapper;
  private final EmailEventPublisher emailEventPublisher;
  private final JwtTokenService jwtTokenService;
  private final String frontendBaseUrl;
  private final String appName;

  /** Creates the user service with its collaborators. */
  public UserService(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      UserResponseMapper responseMapper,
      EmailEventPublisher emailEventPublisher,
      JwtTokenService jwtTokenService,
      String frontendBaseUrl,
      String appName) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.responseMapper = responseMapper;
    this.emailEventPublisher = emailEventPublisher;
    this.jwtTokenService = jwtTokenService;
    this.frontendBaseUrl = frontendBaseUrl;
    this.appName = appName;
  }

  /** Creates a pending user and assigns the requested roles. */
  public Mono<UserResponse> createUser(
      UUID organizationId, UUID actorId, CreateUserRequest request) {
    String email = UserResponseMapper.normalizeEmail(request.email());
    return userRepository
        .findLiveByOrganizationAndEmail(organizationId, email)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("User already exists in the organization"));
              }
              User user = new User(organizationId, email, "", request.displayName());
              return userRepository.save(user);
            })
        .flatMap(
            user ->
                assignRequestedRoles(organizationId, actorId, user, request.roleIds())
                    .then(publishInvitation(user))
                    .thenReturn(user))
        .flatMap(responseMapper::map);
  }

  /** Lists live users of the organization with paging. */
  public Mono<PageResponse<UserResponse>> listUsers(UUID organizationId, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    long offset = (long) safePage * safeSize;
    Mono<Long> count = userRepository.countLiveByOrganization(organizationId);
    Flux<UserResponse> users =
        userRepository
            .listLiveByOrganization(organizationId, safeSize, offset)
            .flatMap(responseMapper::map);
    return count.flatMap(
        total ->
            users.collectList().map(items -> PageResponses.of(items, safePage, safeSize, total)));
  }

  /** Returns a single live user of the organization. */
  public Mono<UserResponse> getUser(UUID organizationId, UUID userId) {
    return userRepository
        .findLiveById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException("User not found")))
        .flatMap(
            user -> {
              if (!user.getOrganizationId().equals(organizationId)) {
                return Mono.error(
                    new ForbiddenException("User does not belong to the organization"));
              }
              return responseMapper.map(user);
            });
  }

  /** Updates the mutable profile fields of a user. */
  public Mono<UserResponse> updateUser(
      UUID organizationId, UUID actorId, UUID userId, UpdateUserRequest request) {
    return requireOrgUser(organizationId, userId)
        .flatMap(
            user -> {
              user.rename(request.displayName(), actorId);
              return userRepository.save(user);
            })
        .flatMap(responseMapper::map);
  }

  /** Changes the lifecycle status of a user. */
  public Mono<UserResponse> changeStatus(
      UUID organizationId, UUID actorId, UUID userId, ChangeUserStatusRequest request) {
    UserStatus target;
    try {
      target = UserStatus.valueOf(request.status());
    } catch (IllegalArgumentException e) {
      return Mono.error(
          new com.integrity.exception.ValidationFailedException(
              "Unknown user status: " + request.status()));
    }
    if (target == UserStatus.PENDING) {
      return Mono.error(
          new com.integrity.exception.ValidationFailedException("Status cannot be set to PENDING"));
    }
    return requireOrgUser(organizationId, userId)
        .flatMap(
            user -> {
              applyStatus(user, target, actorId);
              return userRepository.save(user);
            })
        .flatMap(responseMapper::map);
  }

  private void applyStatus(User user, UserStatus target, UUID actorId) {
    switch (target) {
      case ACTIVE -> {
        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DISABLED) {
          user.unlock(actorId);
        } else {
          user.activate();
        }
      }
      case DISABLED -> user.disable(actorId);
      case LOCKED -> user.lock();
      case PENDING -> throw new IllegalStateException("PENDING handled by caller");
    }
  }

  /** Soft deletes a user. */
  public Mono<Void> deleteUser(UUID organizationId, UUID actorId, UUID userId) {
    return requireOrgUser(organizationId, userId)
        .flatMap(
            user -> {
              user.delete(actorId);
              return userRepository.save(user).then();
            });
  }

  /** Assigns additional roles to a user. */
  public Mono<UserResponse> assignRoles(
      UUID organizationId, UUID actorId, UUID userId, AssignRolesRequest request) {
    return requireOrgUser(organizationId, userId)
        .flatMap(
            user ->
                assignRequestedRoles(organizationId, actorId, user, request.roleIds())
                    .thenReturn(user))
        .flatMap(responseMapper::map);
  }

  private Mono<Void> assignRequestedRoles(
      UUID organizationId, UUID actorId, User user, List<UUID> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      return Mono.empty();
    }
    return Flux.fromIterable(roleIds)
        .flatMap(
            roleId ->
                roleRepository
                    .findLiveById(roleId)
                    .switchIfEmpty(Mono.error(new NotFoundException("Role not found")))
                    .flatMap(
                        role -> {
                          if (!role.getOrganizationId().equals(organizationId)) {
                            return Mono.error(
                                new ForbiddenException("Role does not belong to the organization"));
                          }
                          return userRoleRepository.assign(user.getId(), role.getId(), actorId);
                        }))
        .then();
  }

  private Mono<User> requireOrgUser(UUID organizationId, UUID userId) {
    return userRepository
        .findLiveById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException("User not found")))
        .flatMap(
            user -> {
              if (!user.getOrganizationId().equals(organizationId)) {
                return Mono.error(
                    new ForbiddenException("User does not belong to the organization"));
              }
              return Mono.just(user);
            });
  }

  private Mono<Void> publishInvitation(User user) {
    String token =
        jwtTokenService.issuePurposeToken(PURPOSE_INVITATION, user.getId(), INVITATION_TOKEN_TTL);
    String acceptUrl = frontendBaseUrl + "/accept-invitation?token=" + token;
    return emailEventPublisher.publish(
        new IdentityEmailEvent(
            user.getId(),
            user.getOrganizationId(),
            user.getEmail(),
            user.getDisplayName(),
            DEFAULT_LOCALE,
            "user-invitation",
            Map.of(
                "acceptUrl",
                acceptUrl,
                "expiresInDays",
                String.valueOf(INVITATION_TOKEN_TTL.toDays()),
                "appName",
                appName),
            Instant.now()));
  }
}
