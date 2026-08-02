package com.interviewintegrity.recruiter.web;

import com.interviewintegrity.recruiter.domain.RecruiterStatus;
import com.interviewintegrity.recruiter.service.RecruiterMapper;
import com.interviewintegrity.recruiter.service.RecruiterService;
import com.interviewintegrity.recruiter.web.dto.ChangeRecruiterStatusRequest;
import com.interviewintegrity.recruiter.web.dto.CreateRecruiterRequest;
import com.interviewintegrity.recruiter.web.dto.RecruiterResponse;
import com.interviewintegrity.recruiter.web.dto.UpdateRecruiterRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Recruiter profile endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/recruiters")
@Tag(name = "Recruiters", description = "Manage recruiter profiles")
public final class RecruiterController {

  private final RecruiterService recruiterService;
  private final RecruiterMapper mapper;

  /** Creates the controller bound to the recruiter service and mapper. */
  public RecruiterController(RecruiterService recruiterService, RecruiterMapper mapper) {
    this.recruiterService = recruiterService;
    this.mapper = mapper;
  }

  /** Creates a recruiter profile. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a recruiter")
  public Mono<RecruiterResponse> create(
      Authentication authentication, @Valid @RequestBody CreateRecruiterRequest request) {
    UUID organizationId = SecurityPrincipals.organizationId(authentication);
    return recruiterService
        .createRecruiter(
            organizationId,
            request.userId(),
            request.fullName().trim(),
            request.email().trim(),
            request.title(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the recruiters of the organization, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List recruiters")
  public Flux<RecruiterResponse> list(
      Authentication authentication, @RequestParam(required = false) RecruiterStatus status) {
    return recruiterService
        .list(SecurityPrincipals.organizationId(authentication), status)
        .map(mapper::toResponse);
  }

  /** Returns the recruiter profile linked to the caller. */
  @GetMapping("/me")
  @Operation(summary = "Get my recruiter profile")
  public Mono<RecruiterResponse> me(Authentication authentication) {
    return recruiterService
        .getByUser(
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single recruiter. */
  @GetMapping("/{recruiterId}")
  @Operation(summary = "Get a recruiter")
  public Mono<RecruiterResponse> get(
      Authentication authentication, @PathVariable UUID recruiterId) {
    return recruiterService.getById(recruiterId).map(mapper::toResponse);
  }

  /** Updates a recruiter profile. */
  @PatchMapping("/{recruiterId}")
  @Operation(summary = "Update a recruiter")
  public Mono<RecruiterResponse> update(
      Authentication authentication,
      @PathVariable UUID recruiterId,
      @Valid @RequestBody UpdateRecruiterRequest request) {
    return recruiterService
        .update(
            recruiterId,
            SecurityPrincipals.organizationId(authentication),
            request.fullName().trim(),
            request.email().trim(),
            request.title(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Changes the working status of a recruiter. */
  @PostMapping("/{recruiterId}/status")
  @Operation(summary = "Change recruiter status")
  public Mono<RecruiterResponse> changeStatus(
      Authentication authentication,
      @PathVariable UUID recruiterId,
      @Valid @RequestBody ChangeRecruiterStatusRequest request) {
    return recruiterService
        .changeStatus(
            recruiterId,
            SecurityPrincipals.organizationId(authentication),
            request.status(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a recruiter profile. */
  @DeleteMapping("/{recruiterId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a recruiter")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID recruiterId) {
    return recruiterService.delete(
        recruiterId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
