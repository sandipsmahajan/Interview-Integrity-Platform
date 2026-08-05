package com.integrity.integration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.integration.domain.Integration;
import com.integrity.integration.domain.IntegrationStatus;
import com.integrity.integration.repository.IntegrationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the integration service. */
@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {

  @Mock private IntegrationRepository integrationRepository;

  private IntegrationService integrationService;

  @BeforeEach
  void setUp() {
    integrationService = new IntegrationService(integrationRepository);
  }

  @Test
  void createIntegrationSavesWhenProviderIsNew() {
    UUID organizationId = UUID.randomUUID();
    UUID createdBy = UUID.randomUUID();
    when(integrationRepository.findLiveByOrganizationAndProvider(organizationId, "greenhouse"))
        .thenReturn(Mono.empty());
    when(integrationRepository.save(any(Integration.class)))
        .thenAnswer(
            invocation -> {
              Integration integration = invocation.getArgument(0);
              integration.setId(UUID.randomUUID());
              return Mono.just(integration);
            });

    StepVerifier.create(
            integrationService.createIntegration(
                organizationId,
                "greenhouse",
                "Greenhouse ATS",
                "cred://greenhouse/1",
                "{\"url\":\"https://api.greenhouse.io\"}",
                createdBy))
        .assertNext(
            integration -> {
              org.assertj.core.api.Assertions.assertThat(integration.getStatus())
                  .isEqualTo(IntegrationStatus.DISCONNECTED);
              org.assertj.core.api.Assertions.assertThat(integration.getOrganizationId())
                  .isEqualTo(organizationId);
            })
        .verifyComplete();
  }

  @Test
  void createIntegrationRejectsDuplicateProvider() {
    UUID organizationId = UUID.randomUUID();
    Integration existing =
        new Integration(
            organizationId, "greenhouse", "Greenhouse ATS", "cred://greenhouse/1", null, null);
    when(integrationRepository.findLiveByOrganizationAndProvider(organizationId, "greenhouse"))
        .thenReturn(Mono.just(existing));

    StepVerifier.create(
            integrationService.createIntegration(
                organizationId,
                "greenhouse",
                "Greenhouse ATS",
                "cred://greenhouse/1",
                null,
                UUID.randomUUID()))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void connectIntegrationMarksItConnected() {
    UUID organizationId = UUID.randomUUID();
    Integration integration =
        new Integration(
            organizationId, "greenhouse", "Greenhouse ATS", "cred://greenhouse/1", null, null);
    integration.setId(UUID.randomUUID());

    when(integrationRepository.findLiveByIdAndOrganization(integration.getId(), organizationId))
        .thenReturn(Mono.just(integration));
    when(integrationRepository.save(any(Integration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            integrationService.connectIntegration(
                integration.getId(), organizationId, UUID.randomUUID()))
        .assertNext(
            connected ->
                org.assertj.core.api.Assertions.assertThat(connected.getStatus())
                    .isEqualTo(IntegrationStatus.CONNECTED))
        .verifyComplete();
  }

  @Test
  void getIntegrationReturnsNotFoundForUnknownIntegration() {
    UUID integrationId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(integrationRepository.findLiveByIdAndOrganization(integrationId, organizationId))
        .thenReturn(Mono.empty());

    StepVerifier.create(integrationService.getIntegration(integrationId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void deleteIntegrationSoftDeletes() {
    UUID organizationId = UUID.randomUUID();
    Integration integration =
        new Integration(
            organizationId, "greenhouse", "Greenhouse ATS", "cred://greenhouse/1", null, null);
    integration.setId(UUID.randomUUID());

    when(integrationRepository.findLiveByIdAndOrganization(integration.getId(), organizationId))
        .thenReturn(Mono.just(integration));
    when(integrationRepository.save(any(Integration.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            integrationService.deleteIntegration(
                integration.getId(), organizationId, UUID.randomUUID()))
        .verifyComplete();

    verify(integrationRepository).save(any(Integration.class));
  }
}
