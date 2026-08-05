package com.integrity.candidate.web;

import com.integrity.candidate.service.CandidateDocumentService;
import com.integrity.candidate.service.CandidateMapper;
import com.integrity.candidate.web.dto.CandidateDocumentResponse;
import com.integrity.candidate.web.dto.CreateCandidateDocumentRequest;
import com.integrity.security.SecurityPrincipals;
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

/** Candidate document endpoints. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/documents")
@Tag(name = "Candidate Documents", description = "Manage documents attached to candidates")
public final class CandidateDocumentController {

  private final CandidateDocumentService documentService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the document service and mapper. */
  public CandidateDocumentController(
      CandidateDocumentService documentService, CandidateMapper mapper) {
    this.documentService = documentService;
    this.mapper = mapper;
  }

  /** Registers an uploaded document against a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a candidate document")
  public Mono<CandidateDocumentResponse> create(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody CreateCandidateDocumentRequest request) {
    return documentService
        .create(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.storageObjectId(),
            request.name(),
            request.contentType(),
            request.sizeBytes(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the documents of a candidate. */
  @GetMapping
  @Operation(summary = "List candidate documents")
  public Flux<CandidateDocumentResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return documentService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Soft deletes a document. */
  @DeleteMapping("/{documentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a candidate document")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID documentId) {
    return documentService.delete(
        documentId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
