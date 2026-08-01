package com.interviewintegrity.candidate.repository;

import com.interviewintegrity.candidate.domain.Tag;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link Tag} entities. */
public interface TagRepository extends ReactiveCrudRepository<Tag, UUID> {

  /** Resolves whether a tag code already exists within an organization. */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM tags WHERE organization_id = :organizationId "
          + "AND code = :code)")
  Mono<Boolean> existsByOrganizationAndCode(UUID organizationId, String code);

  /** Lists the tags of an organization ordered by code. */
  @Query("SELECT * FROM tags WHERE organization_id = :organizationId ORDER BY code")
  Flux<Tag> listByOrganization(UUID organizationId);
}
