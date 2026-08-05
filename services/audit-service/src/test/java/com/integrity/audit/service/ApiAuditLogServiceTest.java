package com.integrity.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.integrity.audit.domain.ApiAuditLog;
import com.integrity.audit.repository.ApiAuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the API audit log service. */
@ExtendWith(MockitoExtension.class)
class ApiAuditLogServiceTest {

  private static final String POST_METHOD = "POST";

  @Mock private ApiAuditLogRepository apiAuditLogRepository;

  private ApiAuditLogService apiAuditLogService;

  @BeforeEach
  void setUp() {
    apiAuditLogService = new ApiAuditLogService(apiAuditLogRepository);
  }

  @Test
  void listPagesAccessLogEntries() {
    UUID organizationId = UUID.randomUUID();
    ApiAuditLog entry =
        new ApiAuditLog(
            organizationId,
            "GET",
            "/api/v1/candidates",
            200,
            12,
            UUID.randomUUID(),
            "req-1",
            "10.0.0.2",
            Instant.now());
    PageRequest pageable = PageRequest.of(0, 50);

    when(apiAuditLogRepository.listByOrganization(organizationId, pageable))
        .thenReturn(Flux.just(entry));
    when(apiAuditLogRepository.countByOrganization(organizationId)).thenReturn(Mono.just(1L));

    StepVerifier.create(apiAuditLogService.list(organizationId, null, pageable))
        .assertNext(
            page -> {
              assertThat(page.totalElements()).isEqualTo(1);
              assertThat(page.items()).hasSize(1);
              assertThat(page.items().get(0).getMethod()).isEqualTo("GET");
            })
        .verifyComplete();
  }

  @Test
  void listFiltersByMethod() {
    UUID organizationId = UUID.randomUUID();
    ApiAuditLog entry =
        new ApiAuditLog(
            organizationId,
            POST_METHOD,
            "/api/v1/candidates",
            201,
            34,
            UUID.randomUUID(),
            "req-2",
            "10.0.0.3",
            Instant.now());
    PageRequest pageable = PageRequest.of(0, 50);

    when(apiAuditLogRepository.listByOrganizationAndMethod(organizationId, POST_METHOD, pageable))
        .thenReturn(Flux.just(entry));
    when(apiAuditLogRepository.countByOrganizationAndMethod(organizationId, POST_METHOD))
        .thenReturn(Mono.just(1L));

    StepVerifier.create(apiAuditLogService.list(organizationId, POST_METHOD, pageable))
        .assertNext(page -> assertThat(page.totalElements()).isEqualTo(1))
        .verifyComplete();
  }
}
