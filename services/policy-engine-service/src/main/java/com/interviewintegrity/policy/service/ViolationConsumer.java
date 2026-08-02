package com.interviewintegrity.policy.service;

import com.interviewintegrity.event.EventEnvelope;
import com.interviewintegrity.event.KafkaTopics;
import com.interviewintegrity.policy.domain.ViolationSeverity;
import java.util.Locale;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes {@code policy.violation.v1} events published by the telemetry service.
 *
 * <p>Each signal is deduplicated on its natural fingerprint and stored as an open violation.
 * Malformed messages are logged and skipped; one failing message never stops the consumer.
 */
public final class ViolationConsumer implements DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(ViolationConsumer.class);

  private final KafkaReceiver<String, String> receiver;
  private final ObjectMapper objectMapper;
  private final ViolationService violationService;
  private volatile Disposable subscription;

  /** Creates a consumer bound to the given receiver and service. */
  public ViolationConsumer(
      KafkaReceiver<String, String> receiver, ViolationService violationService) {
    this.receiver = receiver;
    this.violationService = violationService;
    this.objectMapper = new ObjectMapper();
  }

  /** Subscribes to the violation topic and starts processing records. */
  public void start() {
    subscription =
        receiver
            .receive()
            .subscribe(
                record ->
                    handle(record)
                        .doOnSuccess(ignored -> record.receiverOffset().acknowledge())
                        .subscribe(
                            ignored -> {},
                            error ->
                                log.error(
                                    "Failed to process violation record at partition {} offset {}: {}",
                                    record.partition(),
                                    record.offset(),
                                    error.getMessage(),
                                    error)),
                error -> log.error("Violation consumer terminated: {}", error.getMessage(), error));
    log.info("Violation consumer subscribed to topic {}", KafkaTopics.POLICY_VIOLATION);
  }

  @Override
  public void destroy() {
    Disposable active = subscription;
    if (active != null) {
      active.dispose();
    }
    log.info("Violation consumer stopped");
  }

  Mono<Void> handle(ConsumerRecord<String, String> record) {
    try {
      EventEnvelope envelope = objectMapper.readValue(record.value(), EventEnvelope.class);
      ViolationSignal signal = objectMapper.readValue(envelope.payload(), ViolationSignal.class);
      UUID organizationId = parseOrganizationId(record.key());
      if (organizationId == null || signal.sessionId() == null) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Skipping violation message without organization or session at partition {} offset {}",
              record.partition(),
              record.offset());
        }
        return Mono.empty();
      }
      return violationService
          .exists(signal.sessionId(), signal.ruleCode(), signal.occurredAt())
          .flatMap(
              alreadyStored ->
                  alreadyStored
                      ? Mono.empty()
                      : violationService
                          .record(
                              organizationId,
                              signal.sessionId(),
                              signal.interviewId(),
                              signal.policyId(),
                              signal.ruleCode(),
                              severity(signal.severity()),
                              signal.message(),
                              signal.evidence(),
                              signal.occurredAt(),
                              signal.detectedBy())
                          .then());
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(
            "Skipping malformed violation message at partition {} offset {}: {}",
            record.partition(),
            record.offset(),
            e.getMessage(),
            e);
      }
      return Mono.empty();
    }
  }

  private ViolationSeverity severity(String raw) {
    if (raw == null || raw.isBlank()) {
      return ViolationSeverity.MEDIUM;
    }
    try {
      return ViolationSeverity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return ViolationSeverity.MEDIUM;
    }
  }

  private static UUID parseOrganizationId(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(key.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
