package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.domain.CandidateProfile;
import com.interviewintegrity.candidate.service.CandidateProfileService;
import com.interviewintegrity.candidate.web.dto.CandidateProfileResponse;
import com.interviewintegrity.candidate.web.dto.UpdateCandidateProfileRequest;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Extended candidate profile endpoints. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/profile")
@Tag(name = "Candidate Profiles", description = "Manage extended candidate profiles")
public final class CandidateProfileController {

  private final CandidateProfileService profileService;

  /** Creates the controller bound to the profile service. */
  public CandidateProfileController(CandidateProfileService profileService) {
    this.profileService = profileService;
  }

  /** Returns the extended profile of a candidate, creating an empty one on first access. */
  @GetMapping
  @Operation(summary = "Get candidate profile")
  public Mono<CandidateProfileResponse> get(
      Authentication authentication, @PathVariable UUID candidateId) {
    return profileService
        .getOrCreate(candidateId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  /** Replaces the extended profile of a candidate. */
  @PutMapping
  @Operation(summary = "Update candidate profile")
  public Mono<CandidateProfileResponse> update(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody UpdateCandidateProfileRequest request) {
    return profileService
        .update(
            candidateId,
            SecurityPrincipals.organizationId(authentication),
            request.headline(),
            request.bio(),
            request.location(),
            request.timezone(),
            request.resumeSummary(),
            request.linkedinUrl(),
            request.githubUrl(),
            request.skills(),
            request.experienceYears(),
            request.attributes())
        .map(this::toResponse);
  }

  private CandidateProfileResponse toResponse(CandidateProfile profile) {
    List<String> skills = Arrays.asList(profile.getSkills());
    return new CandidateProfileResponse(
        profile.getId(),
        profile.getCandidateId(),
        profile.getHeadline(),
        profile.getBio(),
        profile.getLocation(),
        profile.getTimezone(),
        profile.getResumeSummary(),
        profile.getLinkedinUrl(),
        profile.getGithubUrl(),
        skills,
        profile.getExperienceYears(),
        profile.getAttributes(),
        profile.getCreatedAt(),
        profile.getUpdatedAt());
  }
}
