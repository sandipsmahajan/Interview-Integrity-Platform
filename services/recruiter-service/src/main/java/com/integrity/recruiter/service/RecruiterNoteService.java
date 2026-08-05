package com.integrity.recruiter.service;

import com.integrity.exception.NotFoundException;
import com.integrity.recruiter.domain.RecruiterNote;
import com.integrity.recruiter.repository.RecruiterNoteRepository;
import com.integrity.recruiter.repository.RecruiterRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages private notes attached to candidates. */
public class RecruiterNoteService {

  private final RecruiterNoteRepository noteRepository;
  private final RecruiterRepository recruiterRepository;

  /** Wires the service with its repositories. */
  public RecruiterNoteService(
      RecruiterNoteRepository noteRepository, RecruiterRepository recruiterRepository) {
    this.noteRepository = noteRepository;
    this.recruiterRepository = recruiterRepository;
  }

  /** Creates a note for a candidate on behalf of the given authenticated user. */
  @Transactional
  public Mono<RecruiterNote> create(
      UUID organizationId, UUID candidateId, String body, UUID userId) {
    return recruiterRepository
        .findLiveByOrganizationAndUser(organizationId, userId)
        .switchIfEmpty(Mono.error(new NotFoundException("Recruiter profile not found")))
        .flatMap(
            recruiter ->
                noteRepository.save(
                    new RecruiterNote(
                        organizationId, recruiter.getId(), candidateId, body, userId)));
  }

  /** Lists the notes of a candidate. */
  @Transactional(readOnly = true)
  public Flux<RecruiterNote> list(UUID organizationId, UUID candidateId) {
    return noteRepository.listLiveByOrganizationAndCandidate(organizationId, candidateId);
  }

  /** Updates a note body. */
  @Transactional
  public Mono<RecruiterNote> update(UUID noteId, UUID organizationId, String body, UUID byUser) {
    return noteRepository
        .findLiveById(noteId)
        .switchIfEmpty(Mono.error(new NotFoundException("Note not found")))
        .flatMap(note -> assertOrganization(note, organizationId))
        .map(
            note -> {
              note.update(body, byUser);
              return note;
            })
        .flatMap(noteRepository::save);
  }

  /** Soft deletes a note. */
  @Transactional
  public Mono<Void> delete(UUID noteId, UUID organizationId, UUID byUser) {
    return noteRepository
        .findLiveById(noteId)
        .switchIfEmpty(Mono.error(new NotFoundException("Note not found")))
        .flatMap(note -> assertOrganization(note, organizationId))
        .map(
            note -> {
              note.delete(byUser);
              return note;
            })
        .flatMap(noteRepository::save)
        .then();
  }

  private Mono<RecruiterNote> assertOrganization(RecruiterNote note, UUID organizationId) {
    if (!organizationId.equals(note.getOrganizationId())) {
      return Mono.error(new NotFoundException("Note not found"));
    }
    return Mono.just(note);
  }
}
