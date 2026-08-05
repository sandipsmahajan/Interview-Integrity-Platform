package com.integrity.organization.repository;

import com.integrity.organization.domain.Organization;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Organization} entities. */
public interface OrganizationRepository extends ReactiveCrudRepository<Organization, UUID> {

  /** Finds a live organization by id. */
  @Query("SELECT * FROM organizations WHERE id = :id AND deleted_at IS NULL")
  Mono<Organization> findLiveById(UUID id);

  /** Finds a live organization by its unique slug. */
  @Query("SELECT * FROM organizations WHERE slug = :slug AND deleted_at IS NULL")
  Mono<Organization> findLiveBySlug(String slug);

  /** Returns true when a live organization already uses the slug. */
  @Query("SELECT count(*) FROM organizations WHERE slug = :slug AND deleted_at IS NULL")
  Mono<Long> countLiveBySlug(String slug);

  /** Lists live organizations for administration, ordered by name. */
  @Query(
      "SELECT * FROM organizations WHERE deleted_at IS NULL ORDER BY name LIMIT :limit "
          + "OFFSET :offset")
  Flux<Organization> listLive(int limit, long offset);

  /** Counts all live organizations. */
  @Query("SELECT count(*) FROM organizations WHERE deleted_at IS NULL")
  Mono<Long> countLive();
}
