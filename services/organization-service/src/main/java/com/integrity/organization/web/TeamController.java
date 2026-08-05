package com.integrity.organization.web;

import com.integrity.organization.service.TeamService;
import com.integrity.organization.web.dto.AddTeamMemberRequest;
import com.integrity.organization.web.dto.CreateTeamRequest;
import com.integrity.organization.web.dto.TeamMemberResponse;
import com.integrity.organization.web.dto.TeamResponse;
import com.integrity.organization.web.dto.UpdateTeamRequest;
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

/** Team and membership endpoints, scoped to the caller's organization. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Teams", description = "Manage teams and their members")
public final class TeamController {

  private final TeamService teamService;

  /** Creates the controller bound to the team service. */
  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  /** Creates a team in the organization. */
  @PostMapping("/organizations/teams")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a team")
  public Mono<TeamResponse> createTeam(
      Authentication authentication, @Valid @RequestBody CreateTeamRequest request) {
    return teamService.createTeam(
        SecurityPrincipals.organizationId(authentication),
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Lists the teams of the organization. */
  @GetMapping("/organizations/teams")
  @Operation(summary = "List teams")
  public Flux<TeamResponse> listTeams(Authentication authentication) {
    return teamService.listTeams(SecurityPrincipals.organizationId(authentication));
  }

  /** Lists the teams of a department. */
  @GetMapping("/departments/{departmentId}/teams")
  @Operation(summary = "List teams of a department")
  public Flux<TeamResponse> listTeamsByDepartment(
      Authentication authentication, @PathVariable UUID departmentId) {
    return teamService.listTeamsByDepartment(
        SecurityPrincipals.organizationId(authentication), departmentId);
  }

  /** Returns a single team. */
  @GetMapping("/teams/{teamId}")
  @Operation(summary = "Get a team")
  public Mono<TeamResponse> getTeam(Authentication authentication, @PathVariable UUID teamId) {
    return teamService.getTeam(SecurityPrincipals.organizationId(authentication), teamId);
  }

  /** Renames a team. */
  @PatchMapping("/teams/{teamId}")
  @Operation(summary = "Rename a team")
  public Mono<TeamResponse> renameTeam(
      Authentication authentication,
      @PathVariable UUID teamId,
      @Valid @RequestBody UpdateTeamRequest request) {
    return teamService.renameTeam(
        SecurityPrincipals.organizationId(authentication),
        teamId,
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Soft deletes a team. */
  @DeleteMapping("/teams/{teamId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a team")
  public Mono<Void> deleteTeam(Authentication authentication, @PathVariable UUID teamId) {
    return teamService.deleteTeam(
        SecurityPrincipals.organizationId(authentication),
        teamId,
        SecurityPrincipals.userId(authentication));
  }

  /** Adds a user to a team. */
  @PostMapping("/teams/{teamId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add a team member")
  public Mono<TeamMemberResponse> addMember(
      Authentication authentication,
      @PathVariable UUID teamId,
      @Valid @RequestBody AddTeamMemberRequest request) {
    return teamService.addMember(
        SecurityPrincipals.organizationId(authentication),
        teamId,
        SecurityPrincipals.userId(authentication),
        request);
  }

  /** Removes a user from a team. */
  @DeleteMapping("/teams/{teamId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove a team member")
  public Mono<Void> removeMember(
      Authentication authentication, @PathVariable UUID teamId, @PathVariable UUID userId) {
    return teamService.removeMember(
        SecurityPrincipals.organizationId(authentication), teamId, userId);
  }

  /** Lists the members of a team. */
  @GetMapping("/teams/{teamId}/members")
  @Operation(summary = "List team members")
  public Flux<TeamMemberResponse> listMembers(
      Authentication authentication, @PathVariable UUID teamId) {
    return teamService.listMembers(SecurityPrincipals.organizationId(authentication), teamId);
  }
}
