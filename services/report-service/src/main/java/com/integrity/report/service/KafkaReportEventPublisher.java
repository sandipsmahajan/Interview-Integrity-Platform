package com.integrity.report.service;

import com.integrity.event.EventEnvelope;
import com.integrity.event.KafkaTopics;
import com.integrity.event.ReportGeneratedEvent;
import com.integrity.report.domain.Report;
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
 * Kafka backed {@link ReportEventPublisher}.
 *
 * <p>Each event is wrapped in the platform {@link EventEnvelope} and serialized to JSON before
 * being sent to the topic partitioned by organization id.
 */
public final class KafkaReportEventPublisher implements ReportEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaReportEventPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaReportEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishReportGenerated(Report report) {
    Instant occurredAt = Instant.now();
    ReportGeneratedEvent payload =
        new ReportGeneratedEvent(
            report.getId(),
            report.getOrganizationId(),
            report.getType().name(),
            report.getTitle(),
            report.getFormat().name(),
            occurredAt);
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(), "report.generated.v1", serviceName, occurredAt, toJson(payload));
    String key = report.getOrganizationId().toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.REPORT_GENERATED, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published report-generated event for report {} to topic {}",
                    report.getId(),
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
