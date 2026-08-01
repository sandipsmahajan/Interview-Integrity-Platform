package com.interviewintegrity.organization.web;

import com.interviewintegrity.organization.service.OrganizationService;
import com.interviewintegrity.organization.web.dto.ChangeOrganizationStatusRequest;
import com.interviewintegrity.organization.web.dto.CreateOrganizationRequest;
import com.interviewintegrity.organization.web.dto.OrganizationResponse;
import com.interviewintegrity.organization.web.dto.UpdateOrganizationRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Tenant organization management endpoints. */
@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Manage the caller's tenant organization")
public final class OrganizationController {

  private final OrganizationService organizationService;

  /** Creates the controller bound to the organization service. */
  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  /** Creates a new organization. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an organization")
  public Mono<OrganizationResponse> createOrganization(
      Authentication authentication, @Valid @RequestBody CreateOrganizationRequest request) {
    return organizationService.createOrganization(
        SecurityPrincipals.userId(authentication), request);
  }

  /** Returns the organization of the caller. */
  @GetMapping
  @Operation(summary = "Get my organization")
  public Mono<OrganizationResponse> getOrganization(Authentication authentication) {
    return organizationService.getOrganization(SecurityPrincipals.organizationId(authentication));
  }

  /** Updates the mutable profile of the organization. */
  @PatchMapping
  @Operation(summary = "Update my organization")
  public Mono<OrganizationResponse> updateOrganization(
      Authentication authentication, @Valid @RequestBody UpdateOrganizationRequest request) {
    return organizationService.updateOrganization(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Changes the lifecycle status of the organization. */
  @PostMapping("/status")
  @Operation(summary = "Change organization status")
  public Mono<OrganizationResponse> changeStatus(
      Authentication authentication, @Valid @RequestBody ChangeOrganizationStatusRequest request) {
    return organizationService.changeStatus(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Soft deletes the organization. */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete my organization")
  public Mono<Void> deleteOrganization(Authentication authentication) {
    return organizationService.deleteOrganization(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
