package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.service.CandidateMapper;
import com.interviewintegrity.candidate.service.TagService;
import com.interviewintegrity.candidate.web.dto.AttachTagRequest;
import com.interviewintegrity.candidate.web.dto.TagResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
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

/** Endpoints to apply and remove tags on candidates. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/tags")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Candidate Tags",
    description = "Apply tags to candidates")
public final class CandidateTagController {

  private final TagService tagService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the tag service and mapper. */
  public CandidateTagController(TagService tagService, CandidateMapper mapper) {
    this.tagService = tagService;
    this.mapper = mapper;
  }

  /** Applies a tag to a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Apply a tag to a candidate")
  public Mono<TagResponse> attach(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody AttachTagRequest request) {
    return tagService
        .attach(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.tagId(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the tags applied to a candidate. */
  @GetMapping
  @Operation(summary = "List tags applied to a candidate")
  public Flux<TagResponse> list(Authentication authentication, @PathVariable UUID candidateId) {
    return tagService
        .listByCandidate(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Removes a tag from a candidate. */
  @DeleteMapping("/{tagId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove a tag from a candidate")
  public Mono<Void> detach(
      Authentication authentication, @PathVariable UUID candidateId, @PathVariable UUID tagId) {
    return tagService.detach(SecurityPrincipals.organizationId(authentication), candidateId, tagId);
  }
}
