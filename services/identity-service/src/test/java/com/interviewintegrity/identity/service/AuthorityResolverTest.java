package com.interviewintegrity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.interviewintegrity.identity.domain.Role;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.UserRoleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

/** Unit tests for authority resolution from roles and granted permissions. */
@ExtendWith(MockitoExtension.class)
class AuthorityResolverTest {

  @Mock private UserRoleRepository userRoleRepository;
  @Mock private PermissionRepository permissionRepository;

  @Test
  void resolveCombinesRoleAndPermissionAuthorities() {
    UUID userId = UUID.randomUUID();
    UUID adminRoleId = UUID.randomUUID();
    UUID recruiterRoleId = UUID.randomUUID();
    Role adminRole = new Role(UUID.randomUUID(), "ORG_ADMIN", "Admin", "", true);
    Role recruiterRole = new Role(UUID.randomUUID(), "RECRUITER", "Recruiter", "", false);
    adminRole.setId(adminRoleId);
    recruiterRole.setId(recruiterRoleId);

    when(userRoleRepository.findRolesOfUser(userId))
        .thenReturn(Flux.just(adminRole, recruiterRole));
    when(permissionRepository.findCodesByRole(eq(adminRoleId)))
        .thenReturn(Flux.just("user:read", "organization:manage"));
    when(permissionRepository.findCodesByRole(eq(recruiterRoleId)))
        .thenReturn(Flux.just("user:read", "candidate:manage"));

    List<String> authorities =
        new AuthorityResolver(userRoleRepository, permissionRepository).resolve(userId).block();

    assertThat(authorities)
        .containsExactly(
            "ROLE_ORG_ADMIN",
            "ROLE_RECRUITER",
            "candidate:manage",
            "organization:manage",
            "user:read");
  }

  @Test
  void resolveIsEmptyForUserWithoutRoles() {
    UUID userId = UUID.randomUUID();
    when(userRoleRepository.findRolesOfUser(userId)).thenReturn(Flux.empty());

    List<String> authorities =
        new AuthorityResolver(userRoleRepository, permissionRepository).resolve(userId).block();

    assertThat(authorities).isEmpty();
  }

  @Test
  void resolveDeDuplicatesSharedPermissions() {
    UUID userId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    Role role = new Role(UUID.randomUUID(), "RECRUITER", "Recruiter", "", false);
    role.setId(roleId);

    when(userRoleRepository.findRolesOfUser(userId)).thenReturn(Flux.just(role));
    when(permissionRepository.findCodesByRole(eq(roleId)))
        .thenReturn(Flux.just("user:read", "user:read", "candidate:manage"));

    List<String> authorities =
        new AuthorityResolver(userRoleRepository, permissionRepository).resolve(userId).block();

    assertThat(authorities).containsExactly("ROLE_RECRUITER", "candidate:manage", "user:read");
  }
}
