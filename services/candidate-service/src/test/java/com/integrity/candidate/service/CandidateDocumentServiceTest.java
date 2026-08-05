package com.integrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateDocument;
import com.integrity.candidate.repository.CandidateDocumentRepository;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the candidate document service. */
class CandidateDocumentServiceTest {

  private final CandidateDocumentRepository documentRepository =
      Mockito.mock(CandidateDocumentRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private CandidateDocumentService documentService;

  @BeforeEach
  void setUp() {
    documentService = new CandidateDocumentService(documentRepository, candidateService);
  }

  @Test
  void createRegistersDocumentForCandidate() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID storageObjectId = UUID.randomUUID();
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(documentRepository.save(any(CandidateDocument.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            documentService.create(
                organizationId,
                candidateId,
                storageObjectId,
                "resume.pdf",
                "application/pdf",
                2048,
                UUID.randomUUID()))
        .assertNext(
            document -> {
              assertThat(document.getName()).isEqualTo("resume.pdf");
              assertThat(document.getContentType()).isEqualTo("application/pdf");
              assertThat(document.getSizeBytes()).isEqualTo(2048L);
              assertThat(document.getStorageObjectId()).isEqualTo(storageObjectId);
            })
        .verifyComplete();
  }

  @Test
  void deleteReturnsNotFoundForUnknownDocument() {
    UUID documentId = UUID.randomUUID();
    when(documentRepository.findLiveById(documentId)).thenReturn(Mono.empty());

    StepVerifier.create(documentService.delete(documentId, UUID.randomUUID(), UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void deleteRejectsCrossTenantDocument() {
    UUID documentId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    CandidateDocument document =
        new CandidateDocument(
            organizationId,
            candidateId,
            UUID.randomUUID(),
            "resume.pdf",
            "application/pdf",
            100,
            null);
    document.setId(documentId);
    when(documentRepository.findLiveById(documentId)).thenReturn(Mono.just(document));

    StepVerifier.create(documentService.delete(documentId, UUID.randomUUID(), UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  private static Candidate candidate(UUID candidateId, UUID organizationId) {
    Candidate candidate =
        new Candidate(organizationId, null, "a@b.com", "Jane", null, null, UUID.randomUUID());
    candidate.setId(candidateId);
    return candidate;
  }
}
