package com.interviewintegrity.identity.service;

import com.interviewintegrity.event.EventEnvelope;
import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.event.KafkaTopics;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka backed {@link EmailEventPublisher}.
 *
 * <p>Each email request is wrapped in the platform {@link EventEnvelope} and serialized to JSON
 * before being sent to the identity email topic partitioned by user id.
 */
public final class KafkaEmailEventPublisher implements EmailEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaEmailEventPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaEmailEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publish(IdentityEmailEvent event) {
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(),
            KafkaTopics.IDENTITY_EMAIL,
            serviceName,
            event.occurredAt(),
            toJson(event));
    String key = event.userId().toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.IDENTITY_EMAIL, key, toJson(envelope));
    return sender
        .send(Mono.just(SenderRecord.create(record, key)))
        .doOnNext(
            result ->
                log.info(
                    "Published email event for user {} type {} to topic {}",
                    event.userId(),
                    event.notificationType(),
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
