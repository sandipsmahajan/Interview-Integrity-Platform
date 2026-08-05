package com.integrity.candidate.web;

import com.integrity.candidate.domain.CandidateStatus;
import com.integrity.candidate.service.CandidateMapper;
import com.integrity.candidate.service.CandidateService;
import com.integrity.candidate.web.dto.CandidateResponse;
import com.integrity.candidate.web.dto.ChangeCandidateStatusRequest;
import com.integrity.candidate.web.dto.CreateCandidateRequest;
import com.integrity.candidate.web.dto.UpdateCandidateRequest;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Candidate management endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/candidates")
@Tag(name = "Candidates", description = "Manage candidate master records")
public final class CandidateController {

  private final CandidateService candidateService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the candidate service and mapper. */
  public CandidateController(CandidateService candidateService, CandidateMapper mapper) {
    this.candidateService = candidateService;
    this.mapper = mapper;
  }

  /** Creates a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a candidate")
  public Mono<CandidateResponse> create(
      Authentication authentication, @Valid @RequestBody CreateCandidateRequest request) {
    return candidateService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.userId(),
            request.email().trim(),
            request.fullName().trim(),
            request.phone(),
            request.source(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the candidates of the organization, optionally filtered by status. */
  @GetMapping
  @Operation(summary = "List candidates")
  public Flux<CandidateResponse> list(
      Authentication authentication, @RequestParam(required = false) CandidateStatus status) {
    return candidateService
        .list(SecurityPrincipals.organizationId(authentication), status)
        .map(mapper::toResponse);
  }

  /** Returns a single candidate. */
  @GetMapping("/{candidateId}")
  @Operation(summary = "Get a candidate")
  public Mono<CandidateResponse> get(
      Authentication authentication, @PathVariable UUID candidateId) {
    return candidateService
        .getById(candidateId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a candidate. */
  @PatchMapping("/{candidateId}")
  @Operation(summary = "Update a candidate")
  public Mono<CandidateResponse> update(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody UpdateCandidateRequest request) {
    return candidateService
        .update(
            candidateId,
            SecurityPrincipals.organizationId(authentication),
            request.fullName().trim(),
            request.phone(),
            request.source(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Changes the status of a candidate. */
  @PostMapping("/{candidateId}/status")
  @Operation(summary = "Change candidate status")
  public Mono<CandidateResponse> changeStatus(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody ChangeCandidateStatusRequest request) {
    return candidateService
        .changeStatus(
            candidateId,
            SecurityPrincipals.organizationId(authentication),
            request.status(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a candidate. */
  @DeleteMapping("/{candidateId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a candidate")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID candidateId) {
    return candidateService.delete(
        candidateId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
