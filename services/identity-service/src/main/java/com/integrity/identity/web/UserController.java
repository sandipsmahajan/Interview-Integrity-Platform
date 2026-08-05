package com.integrity.identity.web;

import com.integrity.api.PageResponse;
import com.integrity.identity.service.UserService;
import com.integrity.identity.web.dto.AssignRolesRequest;
import com.integrity.identity.web.dto.ChangeUserStatusRequest;
import com.integrity.identity.web.dto.CreateUserRequest;
import com.integrity.identity.web.dto.UpdateUserRequest;
import com.integrity.identity.web.dto.UserResponse;
import com.integrity.security.SecurityPrincipals;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Organization scoped user management endpoints. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Manage users within an organization")
public final class UserController {

  private final UserService userService;

  /** Creates the controller bound to the user service. */
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Creates a new user, typically by invitation. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a user")
  public Mono<UserResponse> createUser(
      Authentication authentication, @Valid @RequestBody CreateUserRequest request) {
    return userService.createUser(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Lists the users of the organization. */
  @GetMapping
  @Operation(summary = "List users")
  public Mono<PageResponse<UserResponse>> listUsers(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return userService.listUsers(SecurityPrincipals.organizationId(authentication), page, size);
  }

  /** Returns a single user. */
  @GetMapping("/{userId}")
  @Operation(summary = "Get a user")
  public Mono<UserResponse> getUser(Authentication authentication, @PathVariable UUID userId) {
    return userService.getUser(SecurityPrincipals.organizationId(authentication), userId);
  }

  /** Updates the mutable profile of a user. */
  @PatchMapping("/{userId}")
  @Operation(summary = "Update a user")
  public Mono<UserResponse> updateUser(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequest request) {
    return userService.updateUser(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        userId,
        request);
  }

  /** Changes the lifecycle status of a user. */
  @PatchMapping("/{userId}/status")
  @Operation(summary = "Change user status")
  public Mono<UserResponse> changeStatus(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeUserStatusRequest request) {
    return userService.changeStatus(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        userId,
        request);
  }

  /** Assigns roles to a user. */
  @PostMapping("/{userId}/roles")
  @Operation(summary = "Assign roles to a user")
  public Mono<UserResponse> assignRoles(
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody AssignRolesRequest request) {
    return userService.assignRoles(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        userId,
        request);
  }

  /** Soft deletes a user. */
  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a user")
  public Mono<Void> deleteUser(Authentication authentication, @PathVariable UUID userId) {
    return userService.deleteUser(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        userId);
  }
}
