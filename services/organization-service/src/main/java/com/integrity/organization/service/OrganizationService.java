package com.integrity.organization.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.organization.domain.Organization;
import com.integrity.organization.domain.OrganizationAddress;
import com.integrity.organization.domain.OrganizationDomain;
import com.integrity.organization.domain.OrganizationStatus;
import com.integrity.organization.repository.OrganizationAddressRepository;
import com.integrity.organization.repository.OrganizationDomainRepository;
import com.integrity.organization.repository.OrganizationRepository;
import com.integrity.organization.web.dto.AddDomainRequest;
import com.integrity.organization.web.dto.AddressResponse;
import com.integrity.organization.web.dto.ChangeOrganizationStatusRequest;
import com.integrity.organization.web.dto.CreateOrganizationRequest;
import com.integrity.organization.web.dto.DomainResponse;
import com.integrity.organization.web.dto.OrganizationResponse;
import com.integrity.organization.web.dto.UpdateAddressRequest;
import com.integrity.organization.web.dto.UpdateOrganizationRequest;
import com.integrity.validation.Assert;
import java.util.Locale;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Lifecycle operations over the tenant organization and its address and domains. */
public final class OrganizationService {

  private final OrganizationRepository organizationRepository;
  private final OrganizationAddressRepository addressRepository;
  private final OrganizationDomainRepository domainRepository;
  private final OrganizationEventPublisher eventPublisher;
  private final OrganizationMapper mapper;

  /** Creates a service bound to the given repositories and event publisher. */
  public OrganizationService(
      OrganizationRepository organizationRepository,
      OrganizationAddressRepository addressRepository,
      OrganizationDomainRepository domainRepository,
      OrganizationEventPublisher eventPublisher,
      OrganizationMapper mapper) {
    this.organizationRepository = organizationRepository;
    this.addressRepository = addressRepository;
    this.domainRepository = domainRepository;
    this.eventPublisher = eventPublisher;
    this.mapper = mapper;
  }

  /** Creates an organization and publishes the registration event. */
  public Mono<OrganizationResponse> createOrganization(
      UUID byUser, CreateOrganizationRequest request) {
    String slug = resolveSlug(request.slug(), request.name());
    return organizationRepository
        .countLiveBySlug(slug)
        .flatMap(
            count -> {
              if (count > 0) {
                return Mono.error(new ConflictException("Slug already in use: " + slug));
              }
              Organization organization =
                  new Organization(
                      request.name().trim(), slug, request.legalName(), request.settings(), byUser);
              return organizationRepository.save(organization);
            })
        .flatMap(
            organization ->
                addressRepository
                    .save(new OrganizationAddress(organization.getId()))
                    .then(eventPublisher.publishOrganizationRegistered(organization))
                    .thenReturn(organization))
        .map(mapper::toResponse);
  }

  /** Returns the organization. */
  public Mono<OrganizationResponse> getOrganization(UUID organizationId) {
    return requireOrganization(organizationId).map(mapper::toResponse);
  }

  /** Updates the mutable profile of the organization. */
  public Mono<OrganizationResponse> updateOrganization(
      UUID organizationId, UUID byUser, UpdateOrganizationRequest request) {
    return requireOrganization(organizationId)
        .flatMap(
            organization -> {
              organization.rename(request.name().trim(), request.legalName(), byUser);
              organization.updateSettings(request.settings(), byUser);
              return organizationRepository.save(organization);
            })
        .map(mapper::toResponse);
  }

  /** Changes the lifecycle status of the organization. */
  public Mono<OrganizationResponse> changeStatus(
      UUID organizationId, UUID byUser, ChangeOrganizationStatusRequest request) {
    return requireOrganization(organizationId)
        .flatMap(
            organization -> {
              OrganizationStatus target = request.status();
              switch (target) {
                case ACTIVE -> organization.activate(byUser);
                case SUSPENDED -> organization.suspend(byUser);
                case CLOSED -> organization.close(byUser);
                case TRIAL -> Assert.isTrue(false, "An organization cannot be reset to TRIAL");
              }
              return organizationRepository.save(organization);
            })
        .map(mapper::toResponse);
  }

  /** Soft deletes the organization. */
  public Mono<Void> deleteOrganization(UUID organizationId, UUID byUser) {
    return requireOrganization(organizationId)
        .flatMap(
            organization -> {
              organization.delete(byUser);
              return organizationRepository.save(organization).then();
            });
  }

  /** Returns the registered billing address of the organization. */
  public Mono<AddressResponse> getAddress(UUID organizationId) {
    return addressRepository
        .findByOrganizationId(organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Organization has no address")))
        .map(mapper::toAddressResponse);
  }

  /** Creates or updates the registered billing address of the organization. */
  public Mono<AddressResponse> updateAddress(UUID organizationId, UpdateAddressRequest request) {
    return addressRepository
        .findByOrganizationId(organizationId)
        .defaultIfEmpty(new OrganizationAddress(organizationId))
        .flatMap(
            address -> {
              address.update(
                  request.line1(),
                  request.line2(),
                  request.city(),
                  request.region(),
                  request.postalCode(),
                  request.countryCode());
              return addressRepository.save(address);
            })
        .map(mapper::toAddressResponse);
  }

  /** Lists the claimed domains of the organization. */
  public Flux<DomainResponse> listDomains(UUID organizationId) {
    return domainRepository.listLiveByOrganization(organizationId).map(mapper::toDomainResponse);
  }

  /** Claims a new email domain for the organization. */
  public Mono<DomainResponse> addDomain(
      UUID organizationId, UUID byUser, AddDomainRequest request) {
    String normalized = request.domain().toLowerCase(Locale.ROOT);
    return domainRepository
        .findLiveByDomain(normalized)
        .hasElement()
        .flatMap(
            claimed -> {
              if (claimed) {
                return Mono.error(new ConflictException("Domain already claimed"));
              }
              return domainRepository
                  .save(new OrganizationDomain(organizationId, normalized, byUser))
                  .map(mapper::toDomainResponse);
            });
  }

  /** Marks a claimed domain as verified. */
  public Mono<DomainResponse> verifyDomain(UUID organizationId, UUID domainId) {
    return requireDomain(organizationId, domainId)
        .flatMap(
            domain -> {
              domain.verify();
              return domainRepository.save(domain);
            })
        .map(mapper::toDomainResponse);
  }

  /** Releases a claimed domain. */
  public Mono<Void> deleteDomain(UUID organizationId, UUID domainId) {
    return requireDomain(organizationId, domainId)
        .flatMap(
            domain -> {
              domain.delete();
              return domainRepository.save(domain).then();
            });
  }

  private Mono<OrganizationDomain> requireDomain(UUID organizationId, UUID domainId) {
    return domainRepository
        .findLiveById(domainId)
        .switchIfEmpty(Mono.error(new NotFoundException("Domain not found")))
        .flatMap(
            domain -> {
              if (!domain.getOrganizationId().equals(organizationId)) {
                return Mono.error(new ConflictException("Domain does not belong to organization"));
              }
              return Mono.just(domain);
            });
  }

  private Mono<Organization> requireOrganization(UUID organizationId) {
    return organizationRepository
        .findLiveById(organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Organization not found")));
  }

  private static String resolveSlug(String slug, String name) {
    if (slug != null && !slug.isBlank()) {
      return slug.toLowerCase(Locale.ROOT);
    }
    return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
  }
}
