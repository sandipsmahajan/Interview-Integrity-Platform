package com.integrity.storage.web;

import com.integrity.security.SecurityPrincipals;
import com.integrity.storage.service.StorageBucketService;
import com.integrity.storage.service.StorageMapper;
import com.integrity.storage.web.dto.BucketResponse;
import com.integrity.storage.web.dto.CreateBucketRequest;
import com.integrity.storage.web.dto.UpdateBucketRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Storage bucket endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1/buckets")
@Tag(name = "Storage Buckets", description = "Manage tenant scoped storage buckets")
public final class StorageBucketController {

  private final StorageBucketService bucketService;
  private final StorageMapper mapper;

  /** Creates the controller bound to the bucket service. */
  public StorageBucketController(StorageBucketService bucketService, StorageMapper mapper) {
    this.bucketService = bucketService;
    this.mapper = mapper;
  }

  /** Creates a storage bucket. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a storage bucket")
  public Mono<BucketResponse> create(
      Authentication authentication, @Valid @RequestBody CreateBucketRequest request) {
    boolean versioningEnabled =
        request.versioningEnabled() == null ? Boolean.TRUE : request.versioningEnabled();
    return bucketService
        .create(
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            versioningEnabled,
            request.policy(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the buckets of the organization. */
  @GetMapping
  @Operation(summary = "List storage buckets")
  public Flux<BucketResponse> list(Authentication authentication) {
    return bucketService
        .list(SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Returns a single bucket. */
  @GetMapping("/{bucketId}")
  @Operation(summary = "Get a storage bucket")
  public Mono<BucketResponse> get(Authentication authentication, @PathVariable UUID bucketId) {
    return bucketService
        .get(bucketId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates a bucket. */
  @PatchMapping("/{bucketId}")
  @Operation(summary = "Update a storage bucket")
  public Mono<BucketResponse> update(
      Authentication authentication,
      @PathVariable UUID bucketId,
      @Valid @RequestBody UpdateBucketRequest request) {
    boolean versioningEnabled =
        request.versioningEnabled() == null ? Boolean.TRUE : request.versioningEnabled();
    return bucketService
        .update(
            bucketId,
            SecurityPrincipals.organizationId(authentication),
            request.name().trim(),
            versioningEnabled,
            request.policy(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a bucket. */
  @DeleteMapping("/{bucketId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a storage bucket")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID bucketId) {
    return bucketService.delete(
        bucketId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
