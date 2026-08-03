package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

/**
 * Resolves the granted authorities of a user.
 *
 * <p>Authorities are the role code prefixed with {@code ROLE_} plus every permission code granted
 * through the roles held by the user.
 */
public final class AuthorityResolver {

  private final UserRoleRepository userRoleRepository;
  private final PermissionRepository permissionRepository;

  /** Creates a resolver bound to the given repositories. */
  public AuthorityResolver(
      UserRoleRepository userRoleRepository, PermissionRepository permissionRepository) {
    this.userRoleRepository = userRoleRepository;
    this.permissionRepository = permissionRepository;
  }

  /** Resolves the sorted distinct authorities of the given user. */
  public Mono<List<String>> resolve(UUID userId) {
    return userRoleRepository
        .findRolesOfUser(userId)
        .flatMap(
            role -> {
              Mono<List<String>> roleAuthority = Mono.just(List.of("ROLE_" + role.getCode()));
              Mono<List<String>> permissionAuthorities =
                  permissionRepository.findCodesByRole(role.getId()).collectList();
              return Mono.zip(
                  roleAuthority,
                  permissionAuthorities,
                  (roleAuth, permissionAuth) ->
                      Stream.concat(roleAuth.stream(), permissionAuth.stream()).toList());
            })
        .collectList()
        .map(lists -> lists.stream().flatMap(List::stream).distinct().sorted().toList());
  }
}
