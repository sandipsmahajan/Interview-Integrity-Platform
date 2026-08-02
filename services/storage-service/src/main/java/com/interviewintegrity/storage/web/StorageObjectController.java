package com.interviewintegrity.storage.web;

import com.interviewintegrity.security.SecurityPrincipals;
import com.interviewintegrity.storage.domain.StorageClass;
import com.interviewintegrity.storage.service.StorageMapper;
import com.interviewintegrity.storage.service.StorageObjectService;
import com.interviewintegrity.storage.web.dto.ObjectResponse;
import com.interviewintegrity.storage.web.dto.ObjectVersionResponse;
import com.interviewintegrity.storage.web.dto.RegisterObjectRequest;
import com.interviewintegrity.storage.web.dto.StorageObjectHistoryResponse;
import com.interviewintegrity.storage.web.dto.UpdateObjectRequest;
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

/** Storage object metadata endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Storage Objects", description = "Manage object metadata and version history")
public final class StorageObjectController {

  private final StorageObjectService objectService;
  private final StorageMapper mapper;

  /** Creates the controller bound to the object service. */
  public StorageObjectController(StorageObjectService objectService, StorageMapper mapper) {
    this.objectService = objectService;
    this.mapper = mapper;
  }

  /** Registers an object in a bucket. */
  @PostMapping("/buckets/{bucketId}/objects")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a storage object")
  public Mono<ObjectResponse> register(
      Authentication authentication,
      @PathVariable UUID bucketId,
      @Valid @RequestBody RegisterObjectRequest request) {
    return objectService
        .register(
            SecurityPrincipals.organizationId(authentication),
            bucketId,
            request.key().trim(),
            request.sizeBytes(),
            request.contentType(),
            request.checksumSha256(),
            request.storageClass(),
            request.storageRef(),
            request.metadata(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the objects of the organization, optionally scoped by bucket or storage class. */
  @GetMapping("/objects")
  @Operation(summary = "List storage objects")
  public Flux<ObjectResponse> list(
      Authentication authentication,
      @RequestParam(required = false) UUID bucketId,
      @RequestParam(required = false) StorageClass storageClass) {
    return objectService
        .list(SecurityPrincipals.organizationId(authentication), bucketId, storageClass)
        .map(mapper::toResponse);
  }

  /** Returns a single object. */
  @GetMapping("/objects/{objectId}")
  @Operation(summary = "Get a storage object")
  public Mono<ObjectResponse> get(Authentication authentication, @PathVariable UUID objectId) {
    return objectService
        .get(objectId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toResponse);
  }

  /** Updates the mutable metadata of an object. */
  @PatchMapping("/objects/{objectId}")
  @Operation(summary = "Update a storage object")
  public Mono<ObjectResponse> update(
      Authentication authentication,
      @PathVariable UUID objectId,
      @Valid @RequestBody UpdateObjectRequest request) {
    return objectService
        .update(
            objectId,
            SecurityPrincipals.organizationId(authentication),
            request.contentType(),
            request.storageClass(),
            request.metadata(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes an object. */
  @DeleteMapping("/objects/{objectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a storage object")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID objectId) {
    return objectService.delete(
        objectId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }

  /** Lists the active versions of an object. */
  @GetMapping("/objects/{objectId}/versions")
  @Operation(summary = "List object versions")
  public Flux<ObjectVersionResponse> versions(
      Authentication authentication, @PathVariable UUID objectId) {
    return objectService
        .listVersions(objectId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toVersionResponse);
  }

  /** Lists the history snapshots of an object. */
  @GetMapping("/objects/{objectId}/history")
  @Operation(summary = "Get storage object history")
  public Flux<StorageObjectHistoryResponse> history(
      Authentication authentication, @PathVariable UUID objectId) {
    return objectService
        .listHistory(objectId, SecurityPrincipals.organizationId(authentication))
        .map(mapper::toHistoryResponse);
  }
}
