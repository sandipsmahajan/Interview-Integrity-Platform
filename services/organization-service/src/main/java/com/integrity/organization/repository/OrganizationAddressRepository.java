package com.integrity.organization.repository;

import com.integrity.organization.domain.OrganizationAddress;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link OrganizationAddress} entities. */
public interface OrganizationAddressRepository
    extends ReactiveCrudRepository<OrganizationAddress, UUID> {

  /** Finds the single address of an organization. */
  Mono<OrganizationAddress> findByOrganizationId(UUID organizationId);
}
