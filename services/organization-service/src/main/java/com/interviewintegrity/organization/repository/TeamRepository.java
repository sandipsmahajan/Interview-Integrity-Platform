package com.interviewintegrity.organization.repository;

import com.interviewintegrity.organization.domain.Team;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Team} entities. */
public interface TeamRepository extends ReactiveCrudRepository<Team, UUID> {

  /** Finds a live team by id. */
  @Query("SELECT * FROM teams WHERE id = :id AND deleted_at IS NULL")
  Mono<Team> findLiveById(UUID id);

  /** Lists the live teams of an organization, ordered by name. */
  @Query(
      "SELECT * FROM teams WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY name")
  Flux<Team> listLiveByOrganization(UUID organizationId);

  /** Lists the live teams of a department, ordered by name. */
  @Query(
      "SELECT * FROM teams WHERE department_id = :departmentId AND deleted_at IS NULL ORDER BY name")
  Flux<Team> listLiveByDepartment(UUID departmentId);

  /** Counts the live teams of an organization. */
  @Query(
      "SELECT count(*) FROM teams WHERE organization_id = :organizationId AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
