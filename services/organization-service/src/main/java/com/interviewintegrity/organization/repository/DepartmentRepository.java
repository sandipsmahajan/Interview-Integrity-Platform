package com.interviewintegrity.organization.repository;

import com.interviewintegrity.organization.domain.Department;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Department} entities. */
public interface DepartmentRepository extends ReactiveCrudRepository<Department, UUID> {

  /** Finds a live department by id. */
  @Query("SELECT * FROM departments WHERE id = :id AND deleted_at IS NULL")
  Mono<Department> findLiveById(UUID id);

  /** Lists the live departments of an organization, ordered by name. */
  @Query(
      "SELECT * FROM departments WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY name")
  Flux<Department> listLiveByOrganization(UUID organizationId);

  /** Lists the live children of a department. */
  @Query(
      "SELECT * FROM departments WHERE parent_id = :parentId AND deleted_at IS NULL ORDER BY name")
  Flux<Department> listLiveByParent(UUID parentId);

  /** Counts the live departments of an organization. */
  @Query(
      "SELECT count(*) FROM departments WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
