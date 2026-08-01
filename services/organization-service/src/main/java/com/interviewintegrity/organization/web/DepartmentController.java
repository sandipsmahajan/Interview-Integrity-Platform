package com.interviewintegrity.organization.web;

import com.interviewintegrity.organization.service.DepartmentService;
import com.interviewintegrity.organization.web.dto.CreateDepartmentRequest;
import com.interviewintegrity.organization.web.dto.DepartmentResponse;
import com.interviewintegrity.organization.web.dto.UpdateDepartmentRequest;
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

/** Department tree endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Departments", description = "Manage the organization department tree")
public final class DepartmentController {

  private final DepartmentService departmentService;

  /** Creates the controller bound to the department service. */
  public DepartmentController(DepartmentService departmentService) {
    this.departmentService = departmentService;
  }

  /** Creates a department in the organization. */
  @PostMapping("/organizations/departments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a department")
  public Mono<DepartmentResponse> createDepartment(
      Authentication authentication, @Valid @RequestBody CreateDepartmentRequest request) {
    return departmentService.createDepartment(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Lists the departments of the organization. */
  @GetMapping("/organizations/departments")
  @Operation(summary = "List departments")
  public Flux<DepartmentResponse> listDepartments(Authentication authentication) {
    return departmentService.listDepartments(SecurityPrincipals.organizationId(authentication));
  }

  /** Returns a single department. */
  @GetMapping("/departments/{departmentId}")
  @Operation(summary = "Get a department")
  public Mono<DepartmentResponse> getDepartment(
      Authentication authentication, @PathVariable UUID departmentId) {
    return departmentService.getDepartment(
        SecurityPrincipals.organizationId(authentication), departmentId);
  }

  /** Renames a department. */
  @PatchMapping("/departments/{departmentId}")
  @Operation(summary = "Rename a department")
  public Mono<DepartmentResponse> renameDepartment(
      Authentication authentication,
      @PathVariable UUID departmentId,
      @Valid @RequestBody UpdateDepartmentRequest request) {
    return departmentService.renameDepartment(
        SecurityPrincipals.organizationId(authentication),
        departmentId,
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Soft deletes a department. */
  @DeleteMapping("/departments/{departmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a department")
  public Mono<Void> deleteDepartment(
      Authentication authentication, @PathVariable UUID departmentId) {
    return departmentService.deleteDepartment(
        SecurityPrincipals.organizationId(authentication),
        departmentId,
        SecurityPrincipals.userId(authentication));
  }
}
