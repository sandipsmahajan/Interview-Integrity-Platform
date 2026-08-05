package com.integrity.policy.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.event.EventEnvelope;
import com.integrity.event.KafkaTopics;
import com.integrity.policy.domain.Violation;
import com.integrity.policy.domain.ViolationSeverity;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for the violation topic consumer. */
class ViolationConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @SuppressWarnings("unchecked")
  private final KafkaReceiver<String, String> receiver = Mockito.mock(KafkaReceiver.class);

  private final ViolationService violationService = Mockito.mock(ViolationService.class);

  private ViolationConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new ViolationConsumer(receiver, violationService);
  }

  private String envelope(String payload) throws Exception {
    return objectMapper.writeValueAsString(
        new EventEnvelope(
            UUID.randomUUID(),
            KafkaTopics.POLICY_VIOLATION,
            "telemetry-service",
            Instant.now(),
            payload));
  }

  private static Violation violation(UUID organizationId, UUID sessionId) {
    return new Violation(
        organizationId,
        sessionId,
        null,
        null,
        "PROCTOR_ALERT",
        ViolationSeverity.MEDIUM,
        "alert",
        "{}",
        Instant.now(),
        "telemetry-service");
  }

  @Test
  void handleStoresNewViolation() throws Exception {
    UUID organizationId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    ViolationSignal signal =
        new ViolationSignal(
            sessionId,
            UUID.randomUUID(),
            null,
            "PROCTOR_ALERT",
            "MEDIUM",
            "alert",
            "{}",
            Instant.now(),
            "telemetry-service");
    when(violationService.exists(signal.sessionId(), signal.ruleCode(), signal.occurredAt()))
        .thenReturn(Mono.just(false));
    when(violationService.record(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(violation(organizationId, sessionId)));

    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.POLICY_VIOLATION,
            0,
            0L,
            organizationId.toString(),
            envelope(objectMapper.writeValueAsString(signal)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(violationService)
        .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void handleSkipsDuplicateViolation() throws Exception {
    UUID organizationId = UUID.randomUUID();
    ViolationSignal signal =
        new ViolationSignal(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "PROCTOR_ALERT",
            "MEDIUM",
            "alert",
            "{}",
            Instant.now(),
            "telemetry-service");
    when(violationService.exists(any(), any(), any())).thenReturn(Mono.just(true));

    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.POLICY_VIOLATION,
            0,
            0L,
            organizationId.toString(),
            envelope(objectMapper.writeValueAsString(signal)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(violationService, never())
        .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void handleSkipsMalformedMessage() {
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.POLICY_VIOLATION, 0, 0L, UUID.randomUUID().toString(), "not json");

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(violationService, never())
        .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void handleSkipsMessageWithoutOrganizationKey() throws Exception {
    ViolationSignal signal =
        new ViolationSignal(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "PROCTOR_ALERT",
            "MEDIUM",
            "alert",
            "{}",
            Instant.now(),
            "telemetry-service");
    ConsumerRecord<String, String> record =
        new ConsumerRecord<>(
            KafkaTopics.POLICY_VIOLATION,
            0,
            0L,
            null,
            envelope(objectMapper.writeValueAsString(signal)));

    StepVerifier.create(consumer.handle(record)).verifyComplete();
    verify(violationService, never())
        .record(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
