package com.interviewintegrity.identity.web;

import com.interviewintegrity.identity.service.PermissionService;
import com.interviewintegrity.identity.web.dto.PermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Read access to the global permission catalog. */
@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "Read the global permission catalog")
public final class PermissionController {

  private final PermissionService permissionService;

  /** Creates the controller bound to the permission service. */
  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  /** Lists all permissions. */
  @GetMapping
  @Operation(summary = "List permissions")
  public Flux<PermissionResponse> listPermissions() {
    return permissionService.listPermissions();
  }
}
