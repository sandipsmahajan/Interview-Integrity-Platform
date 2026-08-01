package com.interviewintegrity.desktopclient.service;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

/**
 * Consumes the platform topics and relays their messages to connected desktop clients.
 *
 * <p>The receiver is subscribed lazily by {@link #start()} and tolerates individual bad records so
 * one malformed message cannot take down the relay.
 */
public class KafkaRelayConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRelayConsumer.class);

  private final KafkaReceiver<String, String> receiver;
  private final RelayService relayService;
  private final AtomicReference<Disposable> subscription = new AtomicReference<>();

  /** Wires the consumer with the receiver and relay service. */
  public KafkaRelayConsumer(KafkaReceiver<String, String> receiver, RelayService relayService) {
    this.receiver = receiver;
    this.relayService = relayService;
  }

  /** Starts consuming the relay topics. Safe to call once at startup. */
  public void start() {
    subscription.updateAndGet(
        current -> {
          if (current != null) {
            return current;
          }
          return receiver
              .receive()
              .subscribe(
                  this::handle,
                  error -> {
                    if (LOGGER.isWarnEnabled()) {
                      LOGGER.warn("Relay consumer stopped: {}", error.getMessage());
                    }
                  });
        });
  }

  private void handle(ReceiverRecord<String, String> record) {
    try {
      String value = record.value();
      if (value != null && !value.isBlank()) {
        relayService.broadcast(value);
      }
      record.receiverOffset().acknowledge();
    } catch (RuntimeException error) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Skipping unrelayable message: {}", error.getMessage());
      }
      record.receiverOffset().acknowledge();
    }
  }

  /** Stops the relay consumer. */
  public void stop() {
    Disposable disposed = subscription.getAndSet(null);
    if (disposed != null) {
      disposed.dispose();
    }
  }
}
