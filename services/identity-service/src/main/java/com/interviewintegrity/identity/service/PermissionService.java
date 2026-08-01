package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.Permission;
import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.web.dto.PermissionResponse;
import reactor.core.publisher.Flux;

/** Read only access to the global permission catalog. */
public final class PermissionService {

  private final PermissionRepository permissionRepository;

  /** Creates the permission service bound to the permission repository. */
  public PermissionService(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  /** Lists all permissions in the catalog. */
  public Flux<PermissionResponse> listPermissions() {
    return permissionRepository.findAllOrdered().map(PermissionService::toResponse);
  }

  private static PermissionResponse toResponse(Permission permission) {
    return new PermissionResponse(
        permission.getId(),
        permission.getCode(),
        permission.getName(),
        permission.getDescription());
  }
}
