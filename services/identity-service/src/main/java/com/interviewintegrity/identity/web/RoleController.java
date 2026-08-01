package com.interviewintegrity.identity.web;

import com.interviewintegrity.identity.service.RoleService;
import com.interviewintegrity.identity.web.dto.CreateRoleRequest;
import com.interviewintegrity.identity.web.dto.GrantPermissionsRequest;
import com.interviewintegrity.identity.web.dto.RoleResponse;
import com.interviewintegrity.identity.web.dto.UpdateRoleRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Organization scoped role management endpoints. */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Manage RBAC roles and permission grants")
public final class RoleController {

  private final RoleService roleService;

  /** Creates the controller bound to the role service. */
  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  /** Creates a role. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a role")
  public Mono<RoleResponse> createRole(
      Authentication authentication, @Valid @RequestBody CreateRoleRequest request) {
    return roleService.createRole(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Lists the roles of the organization. */
  @GetMapping
  @Operation(summary = "List roles")
  public Flux<RoleResponse> listRoles(Authentication authentication) {
    return roleService.listRoles(SecurityPrincipals.organizationId(authentication));
  }

  /** Returns a single role. */
  @GetMapping("/{roleId}")
  @Operation(summary = "Get a role")
  public Mono<RoleResponse> getRole(Authentication authentication, @PathVariable UUID roleId) {
    return roleService.getRole(SecurityPrincipals.organizationId(authentication), roleId);
  }

  /** Updates a role. */
  @PatchMapping("/{roleId}")
  @Operation(summary = "Update a role")
  public Mono<RoleResponse> updateRole(
      Authentication authentication,
      @PathVariable UUID roleId,
      @Valid @RequestBody UpdateRoleRequest request) {
    return roleService.updateRole(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        roleId,
        request);
  }

  /** Grants permissions to a role. */
  @PostMapping("/{roleId}/permissions")
  @Operation(summary = "Grant permissions to a role")
  public Mono<RoleResponse> grantPermissions(
      Authentication authentication,
      @PathVariable UUID roleId,
      @Valid @RequestBody GrantPermissionsRequest request) {
    return roleService.grantPermissions(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        roleId,
        request);
  }

  /** Revokes a permission from a role. */
  @DeleteMapping("/{roleId}/permissions/{permissionId}")
  @Operation(summary = "Revoke a permission from a role")
  public Mono<RoleResponse> revokePermission(
      Authentication authentication, @PathVariable UUID roleId, @PathVariable UUID permissionId) {
    return roleService.revokePermission(
        SecurityPrincipals.organizationId(authentication), roleId, permissionId);
  }

  /** Deletes a role. */
  @DeleteMapping("/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a role")
  public Mono<Void> deleteRole(Authentication authentication, @PathVariable UUID roleId) {
    return roleService.deleteRole(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        roleId);
  }
}
