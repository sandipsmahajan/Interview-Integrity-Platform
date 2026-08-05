package com.integrity.telemetry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.integrity.telemetry.domain.TelemetryEventType;
import com.integrity.telemetry.repository.TelemetryEventTypeRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/** Unit tests for the telemetry event type service. */
class TelemetryEventTypeServiceTest {

  private final TelemetryEventTypeRepository eventTypeRepository =
      Mockito.mock(TelemetryEventTypeRepository.class);

  private TelemetryEventTypeService eventTypeService;

  @BeforeEach
  void setUp() {
    eventTypeService = new TelemetryEventTypeService(eventTypeRepository);
  }

  @Test
  void listReturnsCatalogEntries() {
    TelemetryEventType type =
        new TelemetryEventType(
            UUID.randomUUID(),
            "KEYSTROKE",
            "Keystroke",
            "Keyboard events",
            90,
            java.time.Instant.now(),
            java.time.Instant.now(),
            1);
    when(eventTypeRepository.list()).thenReturn(Flux.just(type));

    StepVerifier.create(eventTypeService.list())
        .assertNext(
            entry -> {
              assertThat(entry.getCode()).isEqualTo("KEYSTROKE");
              assertThat(entry.getRetentionDays()).isEqualTo(90);
            })
        .verifyComplete();
  }

  @Test
  void listIsEmptyWhenCatalogIsEmpty() {
    when(eventTypeRepository.list()).thenReturn(Flux.empty());

    StepVerifier.create(eventTypeService.list()).verifyComplete();
  }
}
