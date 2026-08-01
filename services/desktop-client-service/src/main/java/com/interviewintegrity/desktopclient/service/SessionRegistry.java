package com.interviewintegrity.desktopclient.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/**
 * Tracks connected desktop client WebSocket sessions and their outbound message sinks.
 *
 * <p>Each connected session owns a hot sink that the relay fan-out pushes into; the WebSocket
 * handler forwards the sink to the client.
 */
public class SessionRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(SessionRegistry.class);

  private final Map<String, Sinks.Many<String>> sessions = new ConcurrentHashMap<>();

  /** Registers a session and returns its outbound sink. */
  public Sinks.Many<String> register(String sessionId) {
    Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
    sessions.put(sessionId, sink);
    return sink;
  }

  /** Removes a session from the registry. */
  public void unregister(String sessionId) {
    Sinks.Many<String> removed = sessions.remove(sessionId);
    if (removed != null) {
      removed.tryEmitComplete();
    }
  }

  /** Broadcasts a payload to every connected session. */
  public void broadcast(String payload) {
    for (Sinks.Many<String> sink : sessions.values()) {
      Sinks.EmitResult result = sink.tryEmitNext(payload);
      if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
        LOGGER.debug("Dropped relay message for a desktop session: {}", result);
      }
    }
  }

  /** Returns the number of connected sessions. */
  public int size() {
    return sessions.size();
  }
}
