package com.integrity.organization.service;

import com.integrity.exception.NotFoundException;
import com.integrity.organization.domain.Team;
import com.integrity.organization.repository.DepartmentRepository;
import com.integrity.organization.repository.TeamMemberRepository;
import com.integrity.organization.repository.TeamRepository;
import com.integrity.organization.web.dto.AddTeamMemberRequest;
import com.integrity.organization.web.dto.CreateTeamRequest;
import com.integrity.organization.web.dto.TeamMemberResponse;
import com.integrity.organization.web.dto.TeamResponse;
import com.integrity.organization.web.dto.UpdateTeamRequest;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CRUD operations over tenant teams and their members. */
public final class TeamService {

  private final TeamRepository teamRepository;
  private final DepartmentRepository departmentRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final OrganizationMapper mapper;

  /** Creates a service bound to the given repositories. */
  public TeamService(
      TeamRepository teamRepository,
      DepartmentRepository departmentRepository,
      TeamMemberRepository teamMemberRepository,
      OrganizationMapper mapper) {
    this.teamRepository = teamRepository;
    this.departmentRepository = departmentRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.mapper = mapper;
  }

  /** Creates a team within a department of the organization. */
  public Mono<TeamResponse> createTeam(
      UUID organizationId, UUID byUser, CreateTeamRequest request) {
    return validateDepartment(organizationId, request.departmentId())
        .then(
            teamRepository
                .save(
                    new Team(organizationId, request.departmentId(), request.name().trim(), byUser))
                .map(mapper::toResponse));
  }

  /** Lists the teams of the organization. */
  public Flux<TeamResponse> listTeams(UUID organizationId) {
    return teamRepository.listLiveByOrganization(organizationId).map(mapper::toResponse);
  }

  /** Lists the teams within a department. */
  public Flux<TeamResponse> listTeamsByDepartment(UUID organizationId, UUID departmentId) {
    return validateDepartment(organizationId, departmentId)
        .thenMany(teamRepository.listLiveByDepartment(departmentId).map(mapper::toResponse));
  }

  /** Returns a single team. */
  public Mono<TeamResponse> getTeam(UUID organizationId, UUID teamId) {
    return requireTeam(organizationId, teamId).map(mapper::toResponse);
  }

  /** Renames a team. */
  public Mono<TeamResponse> renameTeam(
      UUID organizationId, UUID teamId, UUID byUser, UpdateTeamRequest request) {
    return requireTeam(organizationId, teamId)
        .flatMap(
            team -> {
              team.rename(request.name().trim(), byUser);
              return teamRepository.save(team);
            })
        .map(mapper::toResponse);
  }

  /** Soft deletes a team. */
  public Mono<Void> deleteTeam(UUID organizationId, UUID teamId, UUID byUser) {
    return requireTeam(organizationId, teamId)
        .flatMap(
            team -> {
              team.delete(byUser);
              return teamRepository.save(team).then();
            });
  }

  /** Adds a user to a team. */
  public Mono<TeamMemberResponse> addMember(
      UUID organizationId, UUID teamId, UUID byUser, AddTeamMemberRequest request) {
    return requireTeam(organizationId, teamId)
        .then(teamMemberRepository.add(teamId, request.userId(), byUser))
        .then(
            Mono.fromSupplier(
                () -> new TeamMemberResponse(teamId, request.userId(), byUser, Instant.now())));
  }

  /** Removes a user from a team. */
  public Mono<Void> removeMember(UUID organizationId, UUID teamId, UUID userId) {
    return requireTeam(organizationId, teamId).then(teamMemberRepository.remove(teamId, userId));
  }

  /** Lists the members of a team. */
  public Flux<TeamMemberResponse> listMembers(UUID organizationId, UUID teamId) {
    return requireTeam(organizationId, teamId)
        .thenMany(teamMemberRepository.listByTeam(teamId).map(mapper::toMemberResponse));
  }

  private Mono<Void> validateDepartment(UUID organizationId, UUID departmentId) {
    if (departmentId == null) {
      return Mono.empty();
    }
    return departmentRepository
        .findLiveById(departmentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Department not found")))
        .flatMap(
            department -> {
              if (!department.getOrganizationId().equals(organizationId)) {
                return Mono.error(new NotFoundException("Department not found"));
              }
              return Mono.empty();
            });
  }

  private Mono<Team> requireTeam(UUID organizationId, UUID teamId) {
    return teamRepository
        .findLiveById(teamId)
        .switchIfEmpty(Mono.error(new NotFoundException("Team not found")))
        .flatMap(
            team -> {
              if (!team.getOrganizationId().equals(organizationId)) {
                return Mono.error(new NotFoundException("Team not found"));
              }
              return Mono.just(team);
            });
  }
}
