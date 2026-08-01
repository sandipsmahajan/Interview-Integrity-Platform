package com.interviewintegrity.organization.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.organization.domain.Team;
import com.interviewintegrity.organization.domain.TeamMember;
import com.interviewintegrity.organization.repository.DepartmentRepository;
import com.interviewintegrity.organization.repository.TeamMemberRepository;
import com.interviewintegrity.organization.repository.TeamRepository;
import com.interviewintegrity.organization.web.dto.AddTeamMemberRequest;
import com.interviewintegrity.organization.web.dto.CreateTeamRequest;
import com.interviewintegrity.organization.web.dto.TeamMemberResponse;
import com.interviewintegrity.organization.web.dto.TeamResponse;
import com.interviewintegrity.organization.web.dto.UpdateTeamRequest;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CRUD operations over tenant teams and their members. */
public final class TeamService {

  private final TeamRepository teamRepository;
  private final DepartmentRepository departmentRepository;
  private final TeamMemberRepository teamMemberRepository;

  /** Creates a service bound to the given repositories. */
  public TeamService(
      TeamRepository teamRepository,
      DepartmentRepository departmentRepository,
      TeamMemberRepository teamMemberRepository) {
    this.teamRepository = teamRepository;
    this.departmentRepository = departmentRepository;
    this.teamMemberRepository = teamMemberRepository;
  }

  /** Creates a team within a department of the organization. */
  public Mono<TeamResponse> createTeam(
      UUID organizationId, UUID byUser, CreateTeamRequest request) {
    return validateDepartment(organizationId, request.departmentId())
        .then(
            teamRepository
                .save(
                    new Team(organizationId, request.departmentId(), request.name().trim(), byUser))
                .map(this::toResponse));
  }

  /** Lists the teams of the organization. */
  public Flux<TeamResponse> listTeams(UUID organizationId) {
    return teamRepository.listLiveByOrganization(organizationId).map(this::toResponse);
  }

  /** Lists the teams within a department. */
  public Flux<TeamResponse> listTeamsByDepartment(UUID organizationId, UUID departmentId) {
    return validateDepartment(organizationId, departmentId)
        .thenMany(teamRepository.listLiveByDepartment(departmentId).map(this::toResponse));
  }

  /** Returns a single team. */
  public Mono<TeamResponse> getTeam(UUID organizationId, UUID teamId) {
    return requireTeam(organizationId, teamId).map(this::toResponse);
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
        .map(this::toResponse);
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
        .thenMany(teamMemberRepository.listByTeam(teamId).map(this::toMemberResponse));
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

  private TeamResponse toResponse(Team team) {
    return new TeamResponse(
        team.getId(),
        team.getOrganizationId(),
        team.getDepartmentId(),
        team.getName(),
        team.getCreatedAt());
  }

  private TeamMemberResponse toMemberResponse(TeamMember member) {
    return new TeamMemberResponse(
        member.getTeamId(), member.getUserId(), member.getAddedBy(), member.getAddedAt());
  }
}
