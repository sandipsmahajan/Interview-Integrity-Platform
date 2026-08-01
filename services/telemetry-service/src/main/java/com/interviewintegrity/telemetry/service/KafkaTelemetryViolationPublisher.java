package com.interviewintegrity.telemetry.service;

import com.interviewintegrity.event.EventEnvelope;
import com.interviewintegrity.event.KafkaTopics;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka backed {@link TelemetryViolationPublisher}.
 *
 * <p>Each violation is wrapped in the platform {@link EventEnvelope} and sent to {@code
 * policy.violation.v1} partitioned by organization id, matching the contract consumed by the policy
 * engine service.
 */
public final class KafkaTelemetryViolationPublisher implements TelemetryViolationPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaTelemetryViolationPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaTelemetryViolationPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishViolation(UUID organizationId, TelemetryViolationEvent event) {
    Instant occurredAt = Instant.now();
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(),
            KafkaTopics.POLICY_VIOLATION,
            serviceName,
            occurredAt,
            toJson(event));
    String key = organizationId.toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.POLICY_VIOLATION, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published violation signal for session {} to topic {}",
                    event.sessionId(),
                    result.recordMetadata().topic()))
        .then();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize violation event payload", e);
    }
  }
}
