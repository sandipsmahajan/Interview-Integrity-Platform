package com.integrity.desktopclient.service;

import com.integrity.event.KafkaTopics;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Bridges desktop client traffic to and from Kafka.
 *
 * <p>Inbound messages from desktop clients are published to the telemetry topic; messages on the
 * watched platform topics are broadcast to all connected desktop sessions.
 */
public class RelayService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RelayService.class);

  /** Topics whose messages are relayed to connected desktop clients. */
  public static final List<String> RELAY_TOPICS =
      List.of(
          KafkaTopics.TELEMETRY_RECEIVED,
          KafkaTopics.POLICY_VIOLATION,
          KafkaTopics.INTERVIEW_STARTED,
          KafkaTopics.INTERVIEW_COMPLETED);

  private final KafkaSender<String, String> sender;
  private final SessionRegistry sessionRegistry;

  /** Wires the relay with its Kafka sender and session registry. */
  public RelayService(KafkaSender<String, String> sender, SessionRegistry sessionRegistry) {
    this.sender = sender;
    this.sessionRegistry = sessionRegistry;
  }

  /** Publishes an inbound desktop payload to the telemetry topic. */
  public Mono<Void> ingest(String payload) {
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.TELEMETRY_RECEIVED, null, payload);
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, null);
    return sender.send(Mono.just(senderRecord)).then();
  }

  /** Fan-outs a platform message to every connected desktop session. */
  public void broadcast(String payload) {
    sessionRegistry.broadcast(payload);
  }

  /** Logs a relay lifecycle event at debug level. */
  public void logRelay(String message) {
    LOGGER.debug(message);
  }
}
