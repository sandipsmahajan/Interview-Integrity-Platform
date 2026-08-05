package com.integrity.recruiter.web;

import com.integrity.recruiter.service.RecruiterMapper;
import com.integrity.recruiter.service.RecruiterProfileService;
import com.integrity.recruiter.web.dto.RecruiterProfileResponse;
import com.integrity.recruiter.web.dto.UpdateRecruiterProfileRequest;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Extended recruiter profile endpoints. */
@RestController
@RequestMapping("/api/v1/recruiters/{recruiterId}/profile")
@Tag(name = "Recruiter Profiles", description = "Manage extended recruiter profiles")
public final class RecruiterProfileController {

  private final RecruiterProfileService profileService;
  private final RecruiterMapper mapper;

  /** Creates the controller bound to the profile service and mapper. */
  public RecruiterProfileController(
      RecruiterProfileService profileService, RecruiterMapper mapper) {
    this.profileService = profileService;
    this.mapper = mapper;
  }

  /** Returns the extended profile of a recruiter. */
  @GetMapping
  @Operation(summary = "Get recruiter profile")
  public Mono<RecruiterProfileResponse> get(
      Authentication authentication, @PathVariable UUID recruiterId) {
    return profileService
        .getOrCreate(recruiterId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Replaces the extended profile of a recruiter. */
  @PutMapping
  @Operation(summary = "Update recruiter profile")
  public Mono<RecruiterProfileResponse> update(
      Authentication authentication,
      @PathVariable UUID recruiterId,
      @Valid @RequestBody UpdateRecruiterProfileRequest request) {
    return profileService
        .update(
            recruiterId,
            SecurityPrincipals.organizationId(authentication),
            request.bio(),
            request.specialties(),
            request.linkedinUrl(),
            request.availability())
        .map(mapper::toResponse);
  }
}
