package com.integrity.recruiter.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.recruiter.domain.RecruiterAssignment;
import com.integrity.recruiter.repository.RecruiterAssignmentRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages explicit recruiter-candidate assignments. */
public class RecruiterAssignmentService {

  private final RecruiterAssignmentRepository assignmentRepository;

  /** Wires the service with its repository. */
  public RecruiterAssignmentService(RecruiterAssignmentRepository assignmentRepository) {
    this.assignmentRepository = assignmentRepository;
  }

  /** Assigns a recruiter to a candidate, rejecting an existing active assignment. */
  @Transactional
  public Mono<RecruiterAssignment> assign(
      UUID organizationId, UUID recruiterId, UUID candidateId, String role, UUID assignedBy) {
    return assignmentRepository
        .findActive(organizationId, candidateId, recruiterId)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Recruiter already assigned to candidate"));
              }
              return assignmentRepository.save(
                  new RecruiterAssignment(
                      organizationId, recruiterId, candidateId, role, assignedBy));
            });
  }

  /** Lists the assignments of a candidate. */
  @Transactional(readOnly = true)
  public Flux<RecruiterAssignment> list(UUID organizationId, UUID candidateId) {
    return assignmentRepository.listByOrganizationAndCandidate(organizationId, candidateId);
  }

  /** Ends an active assignment. */
  @Transactional
  public Mono<RecruiterAssignment> end(UUID assignmentId, UUID organizationId) {
    return assignmentRepository
        .findActiveById(assignmentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Active assignment not found")))
        .flatMap(assignment -> assertOrganization(assignment, organizationId))
        .map(
            assignment -> {
              assignment.end();
              return assignment;
            })
        .flatMap(assignmentRepository::save);
  }

  /** Changes the role of an active assignment. */
  @Transactional
  public Mono<RecruiterAssignment> changeRole(UUID assignmentId, UUID organizationId, String role) {
    return assignmentRepository
        .findActiveById(assignmentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Active assignment not found")))
        .flatMap(assignment -> assertOrganization(assignment, organizationId))
        .map(
            assignment -> {
              assignment.changeRole(role);
              return assignment;
            })
        .flatMap(assignmentRepository::save);
  }

  private Mono<RecruiterAssignment> assertOrganization(
      RecruiterAssignment assignment, UUID organizationId) {
    if (!organizationId.equals(assignment.getOrganizationId())) {
      return Mono.error(new NotFoundException("Active assignment not found"));
    }
    return Mono.just(assignment);
  }
}
