package com.interviewintegrity.telemetry.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.event.EventEnvelope;
import com.interviewintegrity.event.KafkaTopics;
import com.interviewintegrity.telemetry.domain.TelemetrySession;
import com.interviewintegrity.telemetry.domain.TelemetrySessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for the telemetry event consumer. */
class TelemetryEventConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @SuppressWarnings("unchecked")
  private final KafkaReceiver<String, String> receiver = Mockito.mock(KafkaReceiver.class);

  private final TelemetrySessionService sessionService =
      Mockito.mock(TelemetrySessionService.class);
  private final TelemetryEventService eventService = Mockito.mock(TelemetryEventService.class);

  private TelemetryEventConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new TelemetryEventConsumer(receiver, sessionService, eventService);
  }

  private String envelope(String payload) throws Exception {
    return objectMapper.writeValueAsString(
        new EventEnvelope(
            UUID.randomUUID(),
            KafkaTopics.TELEMETRY_RECEIVED,
            "telemetry-service",
            Instant.now(),
            payload));
  }

  @Test
  void handleRefreshesSessionAndStoresEvents() throws Exception {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    TelemetryBatchPayload payload =
        new TelemetryBatchPayload(
            sessionId,
            UUID.randomUUID(),
            null,
            "device",
            "1.0",
            5,
            "ACTIVE",
            Instant.now(),
            List.of(
                new TelemetryEventData(
                    UUID.randomUUID(), "KEYSTROKE", 1, Instant.now(), null, "{}")));
    TelemetrySession session =
        new TelemetrySession(
            sessionId,
            organizationId,
            UUID.randomUUID(),
            null,
            "device",
            "1.0",
            TelemetrySessionStatus.ACTIVE,
            5,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now(),
            1);
    when(sessionService.ensureSession(
            eq(organizationId), eq(sessionId), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(session));
    when(sessionService.changeStatus(eq(organizationId), eq(sessionId), any(), any()))
        .thenReturn(Mono.just(session));
    when(eventService.ingest(eq(organizationId), eq(sessionId), any())).thenReturn(Flux.empty());

    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.TELEMETRY_RECEIVED,
            0,
            0L,
            organizationId.toString(),
            envelope(objectMapper.writeValueAsString(payload)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(eventService).ingest(eq(organizationId), eq(sessionId), any());
  }

  @Test
  void handleAppliesLifecycleTransition() throws Exception {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    TelemetryBatchPayload payload =
        new TelemetryBatchPayload(
            sessionId, UUID.randomUUID(), null, "device", "1.0", 5, "ENDED", Instant.now(), null);
    TelemetrySession session =
        new TelemetrySession(
            sessionId,
            organizationId,
            UUID.randomUUID(),
            null,
            "device",
            "1.0",
            TelemetrySessionStatus.STARTED,
            5,
            Instant.now(),
            null,
            Instant.now(),
            Instant.now(),
            1);
    when(sessionService.ensureSession(
            eq(organizationId), eq(sessionId), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(session));
    when(sessionService.changeStatus(eq(organizationId), eq(sessionId), any(), any()))
        .thenReturn(Mono.just(session));

    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.TELEMETRY_RECEIVED,
            0,
            0L,
            organizationId.toString(),
            envelope(objectMapper.writeValueAsString(payload)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(sessionService).changeStatus(eq(organizationId), eq(sessionId), any(), any());
  }

  @Test
  void handleSkipsMalformedMessage() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.TELEMETRY_RECEIVED, 0, 0L, UUID.randomUUID().toString(), "not json");

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(sessionService, never()).ensureSession(any(), any(), any(), any(), any(), any(), any());
    verify(eventService, never()).ingest(any(), any(), any());
  }

  @Test
  void handleSkipsMessageWithoutSessionId() throws Exception {
    UUID organizationId = UUID.randomUUID();
    TelemetryBatchPayload payload =
        new TelemetryBatchPayload(
            null, UUID.randomUUID(), null, "device", "1.0", 5, null, Instant.now(), null);
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.TELEMETRY_RECEIVED,
            0,
            0L,
            organizationId.toString(),
            envelope(objectMapper.writeValueAsString(payload)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(sessionService, never()).ensureSession(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void handleSkipsMessageWithoutOrganizationKey() throws Exception {
    UUID sessionId = UUID.randomUUID();
    TelemetryBatchPayload payload =
        new TelemetryBatchPayload(
            sessionId, UUID.randomUUID(), null, "device", "1.0", 5, null, Instant.now(), null);
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.TELEMETRY_RECEIVED,
            0,
            0L,
            null,
            envelope(objectMapper.writeValueAsString(payload)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(sessionService, never()).ensureSession(any(), any(), any(), any(), any(), any(), any());
  }
}
