package com.integrity.storage.web;

import com.integrity.security.SecurityPrincipals;
import com.integrity.storage.service.SignedUrlService;
import com.integrity.storage.service.SignedUrlService.SignedUrlGrant;
import com.integrity.storage.service.StorageMapper;
import com.integrity.storage.web.dto.CreateSignedUrlRequest;
import com.integrity.storage.web.dto.SignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Pre-signed URL grant endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Signed URLs", description = "Manage pre-signed URL grants")
public final class SignedUrlController {

  private final SignedUrlService signedUrlService;
  private final StorageMapper mapper;

  /** Creates the controller bound to the signed URL service. */
  public SignedUrlController(SignedUrlService signedUrlService, StorageMapper mapper) {
    this.signedUrlService = signedUrlService;
    this.mapper = mapper;
  }

  /** Issues a signed URL grant for an object. */
  @PostMapping("/objects/{objectId}/signed-urls")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Issue a pre-signed URL grant")
  public Mono<SignedUrlResponse> create(
      Authentication authentication,
      @PathVariable UUID objectId,
      @Valid @RequestBody CreateSignedUrlRequest request) {
    return signedUrlService
        .create(
            objectId,
            SecurityPrincipals.organizationId(authentication),
            request.purpose(),
            request.expiresAt(),
            request.maxUses(),
            SecurityPrincipals.userId(authentication))
        .map(this::toGrantResponse);
  }

  /** Lists the signed URL grants of an object. */
  @GetMapping("/objects/{objectId}/signed-urls")
  @Operation(summary = "List signed URL grants")
  public Flux<SignedUrlResponse> list(Authentication authentication, @PathVariable UUID objectId) {
    return signedUrlService
        .list(objectId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single signed URL grant. */
  @GetMapping("/signed-urls/{urlId}")
  @Operation(summary = "Get a signed URL grant")
  public Mono<SignedUrlResponse> get(Authentication authentication, @PathVariable UUID urlId) {
    return signedUrlService
        .get(urlId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Revokes a signed URL grant. */
  @PostMapping("/signed-urls/{urlId}/revoke")
  @Operation(summary = "Revoke a signed URL grant")
  public Mono<SignedUrlResponse> revoke(Authentication authentication, @PathVariable UUID urlId) {
    return signedUrlService
        .revoke(
            urlId,
            SecurityPrincipals.organizationId(authentication),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  private SignedUrlResponse toGrantResponse(SignedUrlGrant grant) {
    return mapper.toResponse(grant.signedUrl(), grant.token());
  }
}
