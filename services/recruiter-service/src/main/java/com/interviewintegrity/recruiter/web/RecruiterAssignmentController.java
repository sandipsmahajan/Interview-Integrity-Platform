package com.interviewintegrity.recruiter.web;

import com.interviewintegrity.recruiter.domain.RecruiterAssignment;
import com.interviewintegrity.recruiter.service.RecruiterAssignmentService;
import com.interviewintegrity.recruiter.web.dto.AssignRecruiterRequest;
import com.interviewintegrity.recruiter.web.dto.ChangeAssignmentRoleRequest;
import com.interviewintegrity.recruiter.web.dto.RecruiterAssignmentResponse;
import com.interviewintegrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

/** Recruiter assignment endpoints. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Recruiter Assignments", description = "Assign recruiters to candidates")
public final class RecruiterAssignmentController {

  private final RecruiterAssignmentService assignmentService;

  /** Creates the controller bound to the assignment service. */
  public RecruiterAssignmentController(RecruiterAssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  /** Assigns a recruiter to a candidate. */
  @PostMapping("/candidates/{candidateId}/assignments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Assign a recruiter")
  public Mono<RecruiterAssignmentResponse> assign(
      Authentication authentication,
      @PathVariable UUID candidateId,
      @Valid @RequestBody AssignRecruiterRequest request) {
    return assignmentService
        .assign(
            SecurityPrincipals.organizationId(authentication),
            request.recruiterId(),
            candidateId,
            request.role().trim(),
            SecurityPrincipals.userId(authentication))
        .map(this::toResponse);
  }

  /** Lists the assignments of a candidate. */
  @GetMapping("/candidates/{candidateId}/assignments")
  @Operation(summary = "List candidate assignments")
  public Flux<RecruiterAssignmentResponse> list(
      Authentication authentication, @PathVariable UUID candidateId) {
    return assignmentService
        .list(SecurityPrincipals.organizationId(authentication), candidateId)
        .map(this::toResponse);
  }

  /** Changes the role of an active assignment. */
  @PatchMapping("/assignments/{assignmentId}/role")
  @Operation(summary = "Change assignment role")
  public Mono<RecruiterAssignmentResponse> changeRole(
      Authentication authentication,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody ChangeAssignmentRoleRequest request) {
    return assignmentService
        .changeRole(
            assignmentId, SecurityPrincipals.organizationId(authentication), request.role().trim())
        .map(this::toResponse);
  }

  /** Ends an active assignment. */
  @PostMapping("/assignments/{assignmentId}/end")
  @Operation(summary = "End an assignment")
  public Mono<RecruiterAssignmentResponse> end(
      Authentication authentication, @PathVariable UUID assignmentId) {
    return assignmentService
        .end(assignmentId, SecurityPrincipals.organizationId(authentication))
        .map(this::toResponse);
  }

  private RecruiterAssignmentResponse toResponse(RecruiterAssignment assignment) {
    return new RecruiterAssignmentResponse(
        assignment.getId(),
        assignment.getRecruiterId(),
        assignment.getCandidateId(),
        assignment.getRole(),
        assignment.getAssignedAt(),
        assignment.getEndedAt());
  }
}
