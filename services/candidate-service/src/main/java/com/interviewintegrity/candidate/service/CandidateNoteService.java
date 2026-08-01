package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.CandidateNote;
import com.interviewintegrity.candidate.repository.CandidateNoteRepository;
import com.interviewintegrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages collaboration notes attached to candidates. */
public class CandidateNoteService {

  private static final String NOTE_NOT_FOUND = "Note not found";

  private final CandidateNoteRepository noteRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repository and the candidate service. */
  public CandidateNoteService(
      CandidateNoteRepository noteRepository, CandidateService candidateService) {
    this.noteRepository = noteRepository;
    this.candidateService = candidateService;
  }

  /** Creates a note for a candidate on behalf of the given authenticated user. */
  @Transactional
  public Mono<CandidateNote> create(
      UUID organizationId, UUID candidateId, String body, UUID userId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .then(
            noteRepository.save(
                new CandidateNote(organizationId, candidateId, userId, body.trim(), userId)));
  }

  /** Lists the notes of a candidate. */
  @Transactional(readOnly = true)
  public Flux<CandidateNote> list(UUID organizationId, UUID candidateId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .thenMany(noteRepository.listLiveByOrganizationAndCandidate(organizationId, candidateId));
  }

  /** Updates a note body. */
  @Transactional
  public Mono<CandidateNote> update(UUID noteId, UUID organizationId, String body, UUID byUser) {
    return noteRepository
        .findLiveById(noteId)
        .switchIfEmpty(Mono.error(new NotFoundException(NOTE_NOT_FOUND)))
        .flatMap(note -> assertOrganization(note, organizationId))
        .map(
            note -> {
              note.update(body.trim(), byUser);
              return note;
            })
        .flatMap(noteRepository::save);
  }

  /** Pins or unpins a note. */
  @Transactional
  public Mono<CandidateNote> setPinned(
      UUID noteId, UUID organizationId, boolean pinned, UUID byUser) {
    return noteRepository
        .findLiveById(noteId)
        .switchIfEmpty(Mono.error(new NotFoundException(NOTE_NOT_FOUND)))
        .flatMap(note -> assertOrganization(note, organizationId))
        .map(
            note -> {
              note.setPinned(pinned, byUser);
              return note;
            })
        .flatMap(noteRepository::save);
  }

  /** Soft deletes a note. */
  @Transactional
  public Mono<Void> delete(UUID noteId, UUID organizationId, UUID byUser) {
    return noteRepository
        .findLiveById(noteId)
        .switchIfEmpty(Mono.error(new NotFoundException(NOTE_NOT_FOUND)))
        .flatMap(note -> assertOrganization(note, organizationId))
        .map(
            note -> {
              note.delete(byUser);
              return note;
            })
        .flatMap(noteRepository::save)
        .then();
  }

  private Mono<CandidateNote> assertOrganization(CandidateNote note, UUID organizationId) {
    if (!organizationId.equals(note.getOrganizationId())) {
      return Mono.error(new NotFoundException(NOTE_NOT_FOUND));
    }
    return Mono.just(note);
  }
}
