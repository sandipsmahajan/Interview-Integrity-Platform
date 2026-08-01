package com.interviewintegrity.identity.service;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.ForbiddenException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.identity.domain.Role;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.repository.RolePermissionRepository;
import com.interviewintegrity.identity.repository.RoleRepository;
import com.interviewintegrity.identity.web.dto.CreateRoleRequest;
import com.interviewintegrity.identity.web.dto.GrantPermissionsRequest;
import com.interviewintegrity.identity.web.dto.RoleResponse;
import com.interviewintegrity.identity.web.dto.UpdateRoleRequest;
import java.util.Locale;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Role management within an organization including permission grants. */
public final class RoleService {

  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final PermissionRepository permissionRepository;

  /** Creates the role service with its collaborators. */
  public RoleService(
      RoleRepository roleRepository,
      RolePermissionRepository rolePermissionRepository,
      PermissionRepository permissionRepository) {
    this.roleRepository = roleRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.permissionRepository = permissionRepository;
  }

  /** Creates a role in the organization. */
  public Mono<RoleResponse> createRole(
      UUID organizationId, UUID actorId, CreateRoleRequest request) {
    String code = request.code().toUpperCase(Locale.ROOT);
    return roleRepository
        .findLiveByOrganizationAndCode(organizationId, code)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Role already exists in the organization"));
              }
              Role role =
                  new Role(organizationId, code, request.name(), request.description(), false);
              role.setCreatedBy(actorId);
              return roleRepository.save(role);
            })
        .flatMap(this::toResponse);
  }

  /** Lists the roles of the organization. */
  public Flux<RoleResponse> listRoles(UUID organizationId) {
    return roleRepository.listLiveByOrganization(organizationId).flatMap(this::toResponse);
  }

  /** Returns a single role of the organization. */
  public Mono<RoleResponse> getRole(UUID organizationId, UUID roleId) {
    return requireOrgRole(organizationId, roleId).flatMap(this::toResponse);
  }

  /** Updates the mutable fields of a role. */
  public Mono<RoleResponse> updateRole(
      UUID organizationId, UUID actorId, UUID roleId, UpdateRoleRequest request) {
    return requireOrgRole(organizationId, roleId)
        .flatMap(
            role -> {
              role.update(request.name(), request.description(), actorId);
              return roleRepository.save(role);
            })
        .flatMap(this::toResponse);
  }

  /** Soft deletes a role, refusing to delete system roles. */
  public Mono<Void> deleteRole(UUID organizationId, UUID actorId, UUID roleId) {
    return requireOrgRole(organizationId, roleId)
        .flatMap(
            role -> {
              if (role.isSystem()) {
                return Mono.error(new ForbiddenException("System roles cannot be deleted"));
              }
              role.delete(actorId);
              return roleRepository.save(role).then();
            });
  }

  /** Grants the requested permissions to a role. */
  public Mono<RoleResponse> grantPermissions(
      UUID organizationId, UUID actorId, UUID roleId, GrantPermissionsRequest request) {
    return requireOrgRole(organizationId, roleId)
        .flatMap(
            role ->
                Flux.fromIterable(request.permissionIds())
                    .flatMap(
                        permissionId ->
                            permissionRepository
                                .findById(permissionId)
                                .switchIfEmpty(
                                    Mono.error(new NotFoundException("Permission not found")))
                                .flatMap(
                                    permission ->
                                        rolePermissionRepository.grant(
                                            role.getId(), permission.getId(), actorId)))
                    .then(Mono.just(role)))
        .flatMap(this::toResponse);
  }

  /** Revokes a permission from a role. */
  public Mono<RoleResponse> revokePermission(UUID organizationId, UUID roleId, UUID permissionId) {
    return requireOrgRole(organizationId, roleId)
        .flatMap(
            role -> rolePermissionRepository.revoke(role.getId(), permissionId).thenReturn(role))
        .flatMap(this::toResponse);
  }

  private Mono<RoleResponse> toResponse(Role role) {
    return permissionRepository
        .findCodesByRole(role.getId())
        .collectList()
        .map(
            permissionCodes ->
                new RoleResponse(
                    role.getId(),
                    role.getOrganizationId(),
                    role.getCode(),
                    role.getName(),
                    role.getDescription(),
                    role.isSystem(),
                    role.getCreatedAt(),
                    permissionCodes));
  }

  private Mono<Role> requireOrgRole(UUID organizationId, UUID roleId) {
    return roleRepository
        .findLiveById(roleId)
        .switchIfEmpty(Mono.error(new NotFoundException("Role not found")))
        .flatMap(
            role -> {
              if (!role.getOrganizationId().equals(organizationId)) {
                return Mono.error(
                    new ForbiddenException("Role does not belong to the organization"));
              }
              return Mono.just(role);
            });
  }
}
