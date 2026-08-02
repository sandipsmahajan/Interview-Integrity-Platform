package com.interviewintegrity.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.organization.domain.Organization;
import com.interviewintegrity.organization.domain.OrganizationAddress;
import com.interviewintegrity.organization.domain.OrganizationDomain;
import com.interviewintegrity.organization.domain.OrganizationStatus;
import com.interviewintegrity.organization.repository.OrganizationAddressRepository;
import com.interviewintegrity.organization.repository.OrganizationDomainRepository;
import com.interviewintegrity.organization.repository.OrganizationRepository;
import com.interviewintegrity.organization.web.dto.AddDomainRequest;
import com.interviewintegrity.organization.web.dto.ChangeOrganizationStatusRequest;
import com.interviewintegrity.organization.web.dto.CreateOrganizationRequest;
import com.interviewintegrity.organization.web.dto.OrganizationResponse;
import com.interviewintegrity.organization.web.dto.UpdateOrganizationRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/** Unit tests for the organization lifecycle service. */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

  @Mock private OrganizationRepository organizationRepository;
  @Mock private OrganizationAddressRepository addressRepository;
  @Mock private OrganizationDomainRepository domainRepository;
  @Mock private OrganizationEventPublisher eventPublisher;
  private final OrganizationMapper mapper = new OrganizationMapperImpl();

  private OrganizationService organizationService;

  @BeforeEach
  void setUp() {
    organizationService =
        new OrganizationService(
            organizationRepository, addressRepository, domainRepository, eventPublisher, mapper);
  }

  @Test
  void createOrganizationDerivesSlugAndPublishesEvent() {
    UUID byUser = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    CreateOrganizationRequest request =
        new CreateOrganizationRequest("Acme Corp", null, "Acme Inc", null);

    when(organizationRepository.countLiveBySlug("acme-corp")).thenReturn(Mono.just(0L));
    when(organizationRepository.save(any(Organization.class)))
        .thenAnswer(
            invocation -> {
              Organization organization = invocation.getArgument(0);
              organization.setId(organizationId);
              return Mono.just(organization);
            });
    when(addressRepository.save(any(OrganizationAddress.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(eventPublisher.publishOrganizationRegistered(any(Organization.class)))
        .thenReturn(Mono.empty());

    OrganizationResponse response = organizationService.createOrganization(byUser, request).block();

    assertThat(response).isNotNull();
    assertThat(response.slug()).isEqualTo("acme-corp");
    assertThat(response.status()).isEqualTo("TRIAL");
    verify(eventPublisher).publishOrganizationRegistered(any(Organization.class));
  }

  @Test
  void createOrganizationRejectsDuplicateSlug() {
    CreateOrganizationRequest request = new CreateOrganizationRequest("Acme", "acme", null, null);
    when(organizationRepository.countLiveBySlug("acme")).thenReturn(Mono.just(1L));

    assertThatThrownBy(
            () -> organizationService.createOrganization(UUID.randomUUID(), request).block())
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Slug already in use");
  }

  @Test
  void updateOrganizationRenamesAndSaves() {
    UUID organizationId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Organization organization = new Organization("Acme", "acme", null, null, byUser);
    organization.setId(organizationId);

    when(organizationRepository.findLiveById(organizationId)).thenReturn(Mono.just(organization));
    when(organizationRepository.save(any(Organization.class))).thenReturn(Mono.just(organization));

    OrganizationResponse response =
        organizationService
            .updateOrganization(
                organizationId,
                byUser,
                new UpdateOrganizationRequest("Acme Ltd", "Acme Ltd Legal", "{}"))
            .block();

    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Acme Ltd");
    assertThat(response.legalName()).isEqualTo("Acme Ltd Legal");
  }

  @Test
  void getOrganizationReturnsNotFoundForUnknownId() {
    UUID organizationId = UUID.randomUUID();
    when(organizationRepository.findLiveById(organizationId)).thenReturn(Mono.empty());

    assertThatThrownBy(() -> organizationService.getOrganization(organizationId).block())
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void changeStatusSuspendsOrganization() {
    UUID organizationId = UUID.randomUUID();
    UUID byUser = UUID.randomUUID();
    Organization organization = new Organization("Acme", "acme", null, null, byUser);
    organization.setId(organizationId);

    when(organizationRepository.findLiveById(organizationId)).thenReturn(Mono.just(organization));
    when(organizationRepository.save(any(Organization.class))).thenReturn(Mono.just(organization));

    OrganizationResponse response =
        organizationService
            .changeStatus(
                organizationId,
                byUser,
                new ChangeOrganizationStatusRequest(OrganizationStatus.SUSPENDED))
            .block();

    assertThat(response.status()).isEqualTo("SUSPENDED");
  }

  @Test
  void getAddressReturnsNotFoundWhenAbsent() {
    UUID organizationId = UUID.randomUUID();
    when(addressRepository.findByOrganizationId(organizationId)).thenReturn(Mono.empty());

    assertThatThrownBy(() -> organizationService.getAddress(organizationId).block())
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void addDomainRejectsAlreadyClaimedDomain() {
    UUID organizationId = UUID.randomUUID();
    when(domainRepository.findLiveByDomain(eq("example.com")))
        .thenReturn(
            Mono.just(new OrganizationDomain(organizationId, "example.com", UUID.randomUUID())));

    assertThatThrownBy(
            () ->
                organizationService
                    .addDomain(
                        organizationId, UUID.randomUUID(), new AddDomainRequest("example.com"))
                    .block())
        .isInstanceOf(ConflictException.class);
  }
}
