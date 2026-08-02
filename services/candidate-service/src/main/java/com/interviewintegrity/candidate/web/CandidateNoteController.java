package com.interviewintegrity.candidate.web;

import com.interviewintegrity.candidate.service.CandidateMapper;
import com.interviewintegrity.candidate.service.CandidateNoteService;
import com.interviewintegrity.candidate.web.dto.CandidateNoteResponse;
import com.interviewintegrity.candidate.web.dto.CreateCandidateNoteRequest;
import com.interviewintegrity.candidate.web.dto.SetNotePinnedRequest;
import com.interviewintegrity.candidate.web.dto.UpdateCandidateNoteRequest;
import com.interviewintegrity.security.SecurityPrincipals;
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

/** Candidate note endpoints. */
@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/notes")
@Tag(name = "Candidate Notes", description = "Manage notes attached to candidates")
public final class CandidateNoteController {

  private final CandidateNoteService noteService;
  private final CandidateMapper mapper;

  /** Creates the controller bound to the note service and mapper. */
  public CandidateNoteController(CandidateNoteService noteService, CandidateMapper mapper) {
    this.noteService = noteService;
    this.mapper = mapper;
  }

  /** Creates a note for a candidate. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a candidate note")
  public Mono<CandidateNoteResponse> create(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody CreateCandidateNoteRequest request) {
    return noteService
        .create(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.body().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the notes of a candidate. */
  @GetMapping
  @Operation(summary = "List candidate notes")
  public Flux<CandidateNoteResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return noteService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Updates a note. */
  @PatchMapping("/{noteId}")
  @Operation(summary = "Update a candidate note")
  public Mono<CandidateNoteResponse> update(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @PathVariable UUID noteId,
      @Valid @RequestBody UpdateCandidateNoteRequest request) {
    return noteService
        .update(
            noteId,
            SecurityPrincipals.organizationId(authentication),
            request.body().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Pins or unpins a note. */
  @PatchMapping("/{noteId}/pin")
  @Operation(summary = "Pin or unpin a candidate note")
  public Mono<CandidateNoteResponse> setPinned(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @PathVariable UUID noteId,
      @Valid @RequestBody SetNotePinnedRequest request) {
    return noteService
        .setPinned(
            noteId,
            SecurityPrincipals.organizationId(authentication),
            request.pinned(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a note. */
  @DeleteMapping("/{noteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a candidate note")
  public Mono<Void> delete(
      Authentication authentication, @PathVariable UUID candidateId, @PathVariable UUID noteId) {
    return noteService.delete(
        noteId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
