package com.interviewintegrity.identity.repository;

import com.interviewintegrity.identity.domain.User;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link User} entities. */
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

  /** Finds a live user by organization and email (case insensitive). */
  @Query(
      "SELECT * FROM users WHERE organization_id = :organizationId AND lower(email) = "
          + "lower(:email) AND deleted_at IS NULL")
  Mono<User> findLiveByOrganizationAndEmail(UUID organizationId, String email);

  /** Finds all live users matching the email across organizations. */
  @Query("SELECT * FROM users WHERE lower(email) = lower(:email) AND deleted_at IS NULL")
  Flux<User> findLiveByEmail(String email);

  /** Lists live users of an organization with paging. */
  @Query(
      "SELECT * FROM users WHERE organization_id = :organizationId AND deleted_at IS NULL "
          + "ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<User> listLiveByOrganization(UUID organizationId, int limit, long offset);

  /** Counts live users of an organization. */
  @Query(
      "SELECT count(*) FROM users WHERE organization_id = :organizationId AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);

  /** Finds a live user by id. */
  @Query("SELECT * FROM users WHERE id = :id AND deleted_at IS NULL")
  Mono<User> findLiveById(UUID id);

  /** Counts live users matching the email across all organizations. */
  @Query("SELECT count(*) FROM users WHERE lower(email) = lower(:email) AND deleted_at IS NULL")
  Mono<Long> countLiveByEmail(String email);
}
