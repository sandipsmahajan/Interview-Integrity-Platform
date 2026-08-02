package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.repository.PermissionRepository;
import com.interviewintegrity.identity.web.dto.PermissionResponse;
import reactor.core.publisher.Flux;

/** Read only access to the global permission catalog. */
public final class PermissionService {

  private final PermissionRepository permissionRepository;
  private final IdentityMapper mapper;

  /** Creates the permission service bound to the permission repository. */
  public PermissionService(PermissionRepository permissionRepository, IdentityMapper mapper) {
    this.permissionRepository = permissionRepository;
    this.mapper = mapper;
  }

  /** Lists all permissions in the catalog. */
  public Flux<PermissionResponse> listPermissions() {
    return permissionRepository.findAllOrdered().map(mapper::toResponse);
  }
}
