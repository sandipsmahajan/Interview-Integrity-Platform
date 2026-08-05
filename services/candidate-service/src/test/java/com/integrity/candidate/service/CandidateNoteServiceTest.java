package com.integrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateNote;
import com.integrity.candidate.repository.CandidateNoteRepository;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the candidate note service. */
class CandidateNoteServiceTest {

  private final CandidateNoteRepository noteRepository =
      Mockito.mock(CandidateNoteRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private CandidateNoteService noteService;

  @BeforeEach
  void setUp() {
    noteService = new CandidateNoteService(noteRepository, candidateService);
  }

  @Test
  void createAddsNoteForCandidate() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(noteRepository.save(any(CandidateNote.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(noteService.create(organizationId, candidateId, "Great fit", userId))
        .assertNext(
            note -> {
              assertThat(note.getBody()).isEqualTo("Great fit");
              assertThat(note.getAuthorId()).isEqualTo(userId);
              assertThat(note.isPinned()).isFalse();
            })
        .verifyComplete();
  }

  @Test
  void setPinnedMarksNote() {
    UUID noteId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    CandidateNote note =
        new CandidateNote(organizationId, candidateId, UUID.randomUUID(), "body", null);
    note.setId(noteId);
    when(noteRepository.findLiveById(noteId)).thenReturn(Mono.just(note));
    when(noteRepository.save(any(CandidateNote.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(noteService.setPinned(noteId, organizationId, true, UUID.randomUUID()))
        .assertNext(updated -> assertThat(updated.isPinned()).isTrue())
        .verifyComplete();
  }

  @Test
  void deleteReturnsNotFoundForUnknownNote() {
    UUID noteId = UUID.randomUUID();
    when(noteRepository.findLiveById(noteId)).thenReturn(Mono.empty());

    StepVerifier.create(noteService.delete(noteId, UUID.randomUUID(), UUID.randomUUID()))
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
