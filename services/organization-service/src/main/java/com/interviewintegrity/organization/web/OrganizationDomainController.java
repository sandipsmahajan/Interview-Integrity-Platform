package com.interviewintegrity.organization.web;

import com.interviewintegrity.organization.service.OrganizationService;
import com.interviewintegrity.organization.web.dto.AddDomainRequest;
import com.interviewintegrity.organization.web.dto.DomainResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Claimed email domain endpoints for the caller's organization. */
@RestController
@RequestMapping("/api/v1/organizations/domains")
@Tag(name = "Organization domains", description = "Manage claimed email domains")
public final class OrganizationDomainController {

  private final OrganizationService organizationService;

  /** Creates the controller bound to the organization service. */
  public OrganizationDomainController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  /** Lists the claimed domains of the organization. */
  @GetMapping
  @Operation(summary = "List my organization domains")
  public Flux<DomainResponse> listDomains(Authentication authentication) {
    return organizationService.listDomains(SecurityPrincipals.organizationId(authentication));
  }

  /** Claims a new email domain for the organization. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Claim a domain")
  public Mono<DomainResponse> addDomain(
      Authentication authentication, @Valid @RequestBody AddDomainRequest request) {
    return organizationService.addDomain(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Marks a claimed domain as verified. */
  @PostMapping("/{domainId}/verify")
  @Operation(summary = "Verify a domain")
  public Mono<DomainResponse> verifyDomain(
      Authentication authentication, @PathVariable UUID domainId) {
    return organizationService.verifyDomain(
        SecurityPrincipals.organizationId(authentication), domainId);
  }

  /** Releases a claimed domain. */
  @DeleteMapping("/{domainId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Release a domain")
  public Mono<Void> deleteDomain(Authentication authentication, @PathVariable UUID domainId) {
    return organizationService.deleteDomain(
        SecurityPrincipals.organizationId(authentication), domainId);
  }
}
