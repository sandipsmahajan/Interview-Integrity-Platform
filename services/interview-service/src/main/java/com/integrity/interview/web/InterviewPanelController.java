package com.integrity.interview.web;

import com.integrity.interview.service.InterviewPanelService;
import com.integrity.interview.web.dto.AddPanelistRequest;
import com.integrity.interview.web.dto.InterviewPanelResponse;
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

/** Interviewer panel endpoints. */
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/panel")
@Tag(name = "Interview Panels", description = "Manage interview panels")
public final class InterviewPanelController {

  private final InterviewPanelService panelService;

  /** Creates the controller bound to the panel service. */
  public InterviewPanelController(InterviewPanelService panelService) {
    this.panelService = panelService;
  }

  /** Adds an interviewer to the panel. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add an interviewer to the panel")
  public Mono<InterviewPanelResponse> add(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @Valid @RequestBody AddPanelistRequest request) {
    return panelService.addPanelist(
        SecurityPrincipals.organizationId(authentication),
        interviewId,
        request.interviewerId(),
        request.role().trim(),
        SecurityPrincipals.userId(authentication));
  }

  /** Lists the panel of an interview. */
  @GetMapping
  @Operation(summary = "List the panel of an interview")
  public Flux<InterviewPanelResponse> list(
      Authentication authentication, @PathVariable UUID interviewId) {
    return panelService.listPanel(SecurityPrincipals.organizationId(authentication), interviewId);
  }

  /** Removes an interviewer from the panel. */
  @DeleteMapping("/{interviewerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove an interviewer from the panel")
  public Mono<Void> remove(
      Authentication authentication,
      @PathVariable UUID interviewId,
      @PathVariable UUID interviewerId) {
    return panelService.removePanelist(
        SecurityPrincipals.organizationId(authentication), interviewId, interviewerId);
  }
}
