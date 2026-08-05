package com.integrity.organization.web;

import com.integrity.organization.service.OrganizationService;
import com.integrity.organization.web.dto.AddressResponse;
import com.integrity.organization.web.dto.UpdateAddressRequest;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Registered billing address endpoints for the caller's organization. */
@RestController
@RequestMapping("/api/v1/organizations/address")
@Tag(name = "Organization address", description = "Manage the registered billing address")
public final class OrganizationAddressController {

  private final OrganizationService organizationService;

  /** Creates the controller bound to the organization service. */
  public OrganizationAddressController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  /** Returns the registered billing address of the organization. */
  @GetMapping
  @Operation(summary = "Get my organization address")
  public Mono<AddressResponse> getAddress(Authentication authentication) {
    return organizationService.getAddress(SecurityPrincipals.organizationId(authentication));
  }

  /** Creates or updates the registered billing address of the organization. */
  @PutMapping
  @Operation(summary = "Upsert my organization address")
  public Mono<AddressResponse> updateAddress(
      Authentication authentication, @Valid @RequestBody UpdateAddressRequest request) {
    return organizationService.updateAddress(
        SecurityPrincipals.organizationId(authentication), request);
  }
}
