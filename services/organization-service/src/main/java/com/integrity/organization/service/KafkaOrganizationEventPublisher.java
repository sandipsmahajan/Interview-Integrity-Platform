package com.integrity.organization.service;

import com.integrity.event.EventEnvelope;
import com.integrity.event.KafkaTopics;
import com.integrity.event.OrganizationRegisteredEvent;
import com.integrity.organization.domain.Organization;
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
 * Kafka backed {@link OrganizationEventPublisher}.
 *
 * <p>Each event is wrapped in the platform {@link EventEnvelope} and serialized to JSON before
 * being sent to the topic partitioned by organization id.
 */
public final class KafkaOrganizationEventPublisher implements OrganizationEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaOrganizationEventPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaOrganizationEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishOrganizationRegistered(Organization organization) {
    Instant occurredAt = Instant.now();
    OrganizationRegisteredEvent payload =
        new OrganizationRegisteredEvent(
            organization.getId(), organization.getName(), organization.getSlug(), occurredAt);
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(),
            "organization.registered.v1",
            serviceName,
            occurredAt,
            toJson(payload));
    String key = organization.getId().toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.ORGANIZATION_REGISTERED, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published organization-registered event for organization {} to topic {}",
                    organization.getId(),
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
