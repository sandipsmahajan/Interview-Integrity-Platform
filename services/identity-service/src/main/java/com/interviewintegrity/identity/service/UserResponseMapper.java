package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.Role;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import com.interviewintegrity.identity.web.dto.UserResponse;
import java.util.Locale;
import reactor.core.publisher.Mono;

/** Maps {@link User} entities into {@link UserResponse} wire objects. */
public final class UserResponseMapper {

  private final UserRoleRepository userRoleRepository;

  /** Creates a mapper bound to the role bridge repository. */
  public UserResponseMapper(UserRoleRepository userRoleRepository) {
    this.userRoleRepository = userRoleRepository;
  }

  /** Maps the user and resolves its role codes. */
  public Mono<UserResponse> map(User user) {
    return userRoleRepository
        .findRolesOfUser(user.getId())
        .map(Role::getCode)
        .collectList()
        .map(
            roleCodes ->
                new UserResponse(
                    user.getId(),
                    user.getOrganizationId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getStatus().name(),
                    user.getEmailVerifiedAt(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    roleCodes));
  }

  /** Normalizes an email to its canonical lowercase form. */
  public static String normalizeEmail(String email) {
    return email.toLowerCase(Locale.ROOT);
  }
}
