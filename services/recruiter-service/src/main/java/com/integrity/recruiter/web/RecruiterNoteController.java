package com.integrity.recruiter.web;

import com.integrity.recruiter.service.RecruiterMapper;
import com.integrity.recruiter.service.RecruiterNoteService;
import com.integrity.recruiter.web.dto.CreateNoteRequest;
import com.integrity.recruiter.web.dto.RecruiterNoteResponse;
import com.integrity.recruiter.web.dto.UpdateNoteRequest;
import com.integrity.security.SecurityPrincipals;
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
@RequestMapping("/api/v1")
@Tag(name = "Recruiter Notes", description = "Manage private candidate notes")
public final class RecruiterNoteController {

  private final RecruiterNoteService noteService;
  private final RecruiterMapper mapper;

  /** Creates the controller bound to the note service and mapper. */
  public RecruiterNoteController(RecruiterNoteService noteService, RecruiterMapper mapper) {
    this.noteService = noteService;
    this.mapper = mapper;
  }

  /** Creates a note for a candidate. */
  @PostMapping("/candidates/{candidateId}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a candidate note")
  public Mono<RecruiterNoteResponse> create(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody CreateNoteRequest request) {
    return noteService
        .create(
            SecurityPrincipals.organizationId(authentication),
            candidateId,
            request.body().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Lists the notes of a candidate. */
  @GetMapping("/candidates/{candidateId}/notes")
  @Operation(summary = "List candidate notes")
  public Flux<RecruiterNoteResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return noteService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(mapper::toResponse);
  }

  /** Updates a note. */
  @PatchMapping("/notes/{noteId}")
  @Operation(summary = "Update a note")
  public Mono<RecruiterNoteResponse> update(
      Authentication authentication,
      @PathVariable UUID noteId,
      @Valid @RequestBody UpdateNoteRequest request) {
    return noteService
        .update(
            noteId,
            SecurityPrincipals.organizationId(authentication),
            request.body().trim(),
            SecurityPrincipals.userId(authentication))
        .map(mapper::toResponse);
  }

  /** Soft deletes a note. */
  @DeleteMapping("/notes/{noteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a note")
  public Mono<Void> delete(Authentication authentication, @PathVariable UUID noteId) {
    return noteService.delete(
        noteId,
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication));
  }
}
