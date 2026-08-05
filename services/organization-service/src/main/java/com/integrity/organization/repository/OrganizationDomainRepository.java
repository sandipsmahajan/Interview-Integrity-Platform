package com.integrity.organization.repository;

import com.integrity.organization.domain.OrganizationDomain;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link OrganizationDomain} entities. */
public interface OrganizationDomainRepository
    extends ReactiveCrudRepository<OrganizationDomain, UUID> {

  /** Finds a live domain claim by id. */
  @Query("SELECT * FROM organization_domains WHERE id = :id AND deleted_at IS NULL")
  Mono<OrganizationDomain> findLiveById(UUID id);

  /** Finds a live domain claim by its normalized domain name. */
  @Query("SELECT * FROM organization_domains WHERE domain = :domain AND deleted_at IS NULL")
  Mono<OrganizationDomain> findLiveByDomain(String domain);

  /** Lists the live domain claims of an organization. */
  @Query(
      "SELECT * FROM organization_domains WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL ORDER BY domain")
  Flux<OrganizationDomain> listLiveByOrganization(UUID organizationId);

  /** Counts the live domain claims of an organization. */
  @Query(
      "SELECT count(*) FROM organization_domains WHERE organization_id = :organizationId "
          + "AND deleted_at IS NULL")
  Mono<Long> countLiveByOrganization(UUID organizationId);
}
