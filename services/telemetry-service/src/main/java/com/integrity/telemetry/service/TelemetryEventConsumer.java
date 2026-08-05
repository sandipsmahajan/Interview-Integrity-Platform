package com.integrity.telemetry.service;

import com.integrity.event.EventEnvelope;
import com.integrity.event.KafkaTopics;
import com.integrity.observability.MdcCorrelation;
import com.integrity.telemetry.domain.TelemetrySessionStatus;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.retry.Retry;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes {@code telemetry.received.v1} events from the telemetry clients.
 *
 * <p>A single message refreshes the session (lifecycle fields) and stores the carried event batch.
 * Transient processing failures are retried with bounded backoff; records that exhaust retries are
 * routed to the dead-letter topic so a poison message cannot stall the partition. Malformed
 * messages are logged and skipped; one failing message never stops the consumer.
 */
public final class TelemetryEventConsumer implements DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(TelemetryEventConsumer.class);
  private static final int MAX_RETRIES = 3;

  private final KafkaReceiver<String, String> receiver;
  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final TelemetrySessionService sessionService;
  private final TelemetryEventService eventService;
  private volatile Disposable subscription;

  /** Creates a consumer bound to the given receiver and services. */
  public TelemetryEventConsumer(
      KafkaReceiver<String, String> receiver,
      KafkaSender<String, String> sender,
      TelemetrySessionService sessionService,
      TelemetryEventService eventService) {
    this.receiver = receiver;
    this.sender = sender;
    this.sessionService = sessionService;
    this.eventService = eventService;
    this.objectMapper = new ObjectMapper();
  }

  /** Subscribes to the telemetry topic and starts processing records. */
  public void start() {
    subscription =
        receiver
            .receive()
            .subscribe(
                record ->
                    handle(record)
                        .doOnSuccess(ignored -> record.receiverOffset().acknowledge())
                        .retryWhen(
                            Retry.backoff(MAX_RETRIES, Duration.ofMillis(200))
                                .maxBackoff(Duration.ofSeconds(2))
                                .doBeforeRetry(
                                    retrySignal ->
                                        log.warn(
                                            "Retrying telemetry record at partition {} offset {} after attempt {}",
                                            record.partition(),
                                            record.offset(),
                                            retrySignal.totalRetries() + 1)))
                        .onErrorResume(
                            error ->
                                routeToDeadLetter(
                                    record, record.receiverOffset()::acknowledge, error))
                        .subscribe(
                            ignored -> {},
                            error ->
                                log.error(
                                    "Failed to process telemetry record at partition {} offset {}: {}",
                                    record.partition(),
                                    record.offset(),
                                    error.getMessage(),
                                    error)),
                error -> log.error("Telemetry consumer terminated: {}", error.getMessage(), error));
    log.info("Telemetry consumer subscribed to topic {}", KafkaTopics.TELEMETRY_RECEIVED);
  }

  @Override
  public void destroy() {
    Disposable active = subscription;
    if (active != null) {
      active.dispose();
    }
    log.info("Telemetry consumer stopped");
  }

  /**
   * Forwards a record that exhausted its retries to the dead-letter topic, then acknowledges the
   * original offset so the poison message does not stall the partition.
   */
  Mono<Void> routeToDeadLetter(
      ConsumerRecord<String, String> record, Runnable acknowledge, Throwable error) {
    ProducerRecord<String, String> deadLetter =
        new ProducerRecord<>(KafkaTopics.TELEMETRY_RECEIVED_DLQ, record.key(), record.value());
    return sender
        .send(Mono.just(SenderRecord.create(deadLetter, record.key())))
        .doOnNext(
            result -> {
              if (log.isErrorEnabled()) {
                log.error(
                    "Routed telemetry record at partition {} offset {} to dead-letter topic {}: {}",
                    record.partition(),
                    record.offset(),
                    result.recordMetadata().topic(),
                    error.getMessage(),
                    error);
              }
            })
        .then(Mono.fromRunnable(acknowledge));
  }

  Mono<Void> handle(ConsumerRecord<String, String> record) {
    try {
      EventEnvelope envelope = objectMapper.readValue(record.value(), EventEnvelope.class);
      TelemetryBatchPayload payload =
          objectMapper.readValue(envelope.payload(), TelemetryBatchPayload.class);
      UUID organizationId = parseOrganizationId(record.key());
      if (organizationId == null) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Skipping telemetry message without a valid organization key at partition {} offset {}",
              record.partition(),
              record.offset());
        }
        return Mono.empty();
      }
      if (payload.sessionId() == null) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Skipping telemetry message without a session id at partition {} offset {}",
              record.partition(),
              record.offset());
        }
        return Mono.empty();
      }
      Mono<Void> sessionSync =
          sessionService
              .ensureSession(
                  organizationId,
                  payload.sessionId(),
                  payload.interviewId(),
                  payload.candidateId(),
                  payload.deviceId(),
                  payload.clientVersion(),
                  payload.heartbeatCadenceSeconds())
              .flatMap(session -> applyLifecycle(organizationId, session.getId(), payload));
      Mono<Void> eventsSync =
          payload.events() == null || payload.events().isEmpty()
              ? Mono.empty()
              : eventService.ingest(organizationId, payload.sessionId(), payload.events()).then();
      return MdcCorrelation.withCorrelationId(
          sessionSync.then(eventsSync), envelope.eventId().toString());
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(
            "Skipping malformed telemetry message at partition {} offset {}: {}",
            record.partition(),
            record.offset(),
            e.getMessage(),
            e);
      }
      return Mono.empty();
    }
  }

  private Mono<Void> applyLifecycle(
      UUID organizationId, UUID sessionId, TelemetryBatchPayload payload) {
    if (payload.sessionStatus() == null || payload.sessionStatus().isBlank()) {
      return Mono.empty();
    }
    try {
      TelemetrySessionStatus status = TelemetrySessionStatus.valueOf(payload.sessionStatus());
      return sessionService
          .changeStatus(organizationId, sessionId, status, payload.occurredAt())
          .then();
    } catch (IllegalArgumentException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Skipping unknown session status '{}' for session {}",
            payload.sessionStatus(),
            sessionId);
      }
      return Mono.empty();
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
