package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.service.CandidateMapper;
import com.interviewintegrity.candidate.service.TagService;
import com.interviewintegrity.candidate.web.dto.CreateTagRequest;
import com.interviewintegrity.candidate.web.dto.TagResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Tag management endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Manage candidate tags")
public final class TagController {

  private final TagService tagService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the tag service and mapper. */
  public TagController(TagService tagService, CandidateMapper mapper) {
    this.tagService = tagService;
    this.mapper = mapper;
  }

  /** Creates a tag. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a tag")
  public Mono<TagResponse> create(
      Authentication authentication, @Valid @RequestBody CreateTagRequest request) {
    return tagService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.code().trim(),
            request.name().trim())
        .map(mapper::toResponse);
  }

  /** Lists the tags of the organization. */
  @GetMapping
  @Operation(summary = "List tags")
  public Flux<TagResponse> list(Authentication authentication) {
    return tagService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }
}
