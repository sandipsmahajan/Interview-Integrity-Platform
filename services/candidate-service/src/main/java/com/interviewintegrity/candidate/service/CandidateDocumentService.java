package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.CandidateDocument;
import com.interviewintegrity.candidate.repository.CandidateDocumentRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages documents attached to candidates. */
public class CandidateDocumentService {

  private final CandidateDocumentRepository documentRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repository and the candidate service. */
  public CandidateDocumentService(
      CandidateDocumentRepository documentRepository, CandidateService candidateService) {
    this.documentRepository = documentRepository;
    this.candidateService = candidateService;
  }

  /** Registers an uploaded document against a candidate. */
  @Transactional
  public Mono<CandidateDocument> create(
      UUID organizationId,
      UUID candidateId,
      UUID storageObjectId,
      String name,
      String contentType,
      long sizeBytes,
      UUID uploadedBy) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .then(
            documentRepository.save(
                new CandidateDocument(
                    organizationId,
                    candidateId,
                    storageObjectId,
                    name.trim(),
                    contentType,
                    sizeBytes,
                    uploadedBy)));
  }

  /** Lists the live documents of a candidate. */
  @Transactional(readOnly = true)
  public Flux<CandidateDocument> list(UUID organizationId, UUID candidateId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .thenMany(
            documentRepository.listLiveByOrganizationAndCandidate(organizationId, candidateId));
  }

  /** Soft deletes a document. */
  @Transactional
  public Mono<Void> delete(UUID documentId, UUID organizationId, UUID byUser) {
    return documentRepository
        .findLiveById(documentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Document not found")))
        .flatMap(document -> assertOrganization(document, organizationId))
        .map(
            document -> {
              document.delete(byUser);
              return document;
            })
        .flatMap(documentRepository::save)
        .then();
  }

  private Mono<CandidateDocument> assertOrganization(
      CandidateDocument document, UUID organizationId) {
    if (!organizationId.equals(document.getOrganizationId())) {
      return Mono.error(new NotFoundException("Document not found"));
    }
    return Mono.just(document);
  }
}
