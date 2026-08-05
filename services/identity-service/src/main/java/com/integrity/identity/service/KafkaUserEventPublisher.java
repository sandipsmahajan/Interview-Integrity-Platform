package com.integrity.identity.service;

import com.integrity.event.EventEnvelope;
import com.integrity.event.KafkaTopics;
import com.integrity.event.UserRegisteredEvent;
import com.integrity.identity.domain.User;
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
 * Kafka backed {@link UserEventPublisher}.
 *
 * <p>Each event is wrapped in the platform {@link EventEnvelope} and serialized to JSON before
 * being sent to the topic partitioned by user id.
 */
public final class KafkaUserEventPublisher implements UserEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaUserEventPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaUserEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishUserRegistered(User user) {
    Instant occurredAt = Instant.now();
    UserRegisteredEvent payload =
        new UserRegisteredEvent(
            user.getId(), user.getOrganizationId(), user.getEmail(), occurredAt);
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(),
            "identity.user-registered.v1",
            serviceName,
            occurredAt,
            toJson(payload));
    String key = user.getId().toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.IDENTITY_USER_REGISTERED, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published user-registered event for user {} to topic {}",
                    user.getId(),
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
