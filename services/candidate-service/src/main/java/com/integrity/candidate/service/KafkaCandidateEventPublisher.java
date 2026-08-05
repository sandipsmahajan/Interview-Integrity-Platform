package com.integrity.candidate.service;

import com.integrity.candidate.domain.Candidate;
import com.integrity.event.EventEnvelope;
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
 * Kafka backed {@link CandidateEventPublisher}.
 *
 * <p>Each event is wrapped in the platform {@link EventEnvelope} and serialized to JSON before
 * being sent to the topic partitioned by candidate id.
 */
public final class KafkaCandidateEventPublisher implements CandidateEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaCandidateEventPublisher.class);
  private static final String TOPIC = "candidate.registered.v1";
  private static final String EVENT_TYPE = "candidate.registered.v1";

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaCandidateEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishCandidateRegistered(Candidate candidate) {
    Instant occurredAt = Instant.now();
    CandidateRegisteredEvent payload =
        new CandidateRegisteredEvent(
            candidate.getId(),
            candidate.getOrganizationId(),
            candidate.getEmail(),
            candidate.getFullName(),
            occurredAt);
    EventEnvelope envelope =
        new EventEnvelope(UUID.randomUUID(), EVENT_TYPE, serviceName, occurredAt, toJson(payload));
    String key = candidate.getId().toString();
    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published candidate-registered event for candidate {} to topic {}",
                    candidate.getId(),
                    result.recordMetadata().topic()))
        .then();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize event payload", e);
    }
  }
}
