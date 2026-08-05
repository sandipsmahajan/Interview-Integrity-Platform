package com.integrity.desktopclient.web;

import com.integrity.desktopclient.service.RelayService;
import com.integrity.desktopclient.service.SessionRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * WebSocket endpoint for desktop clients.
 *
 * <p>Inbound text messages are forwarded to the platform via {@link RelayService}; platform
 * messages consumed from Kafka are written back to the client through its outbound sink.
 */
public class DesktopWebSocketHandler implements WebSocketHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DesktopWebSocketHandler.class);

  private final SessionRegistry sessionRegistry;
  private final RelayService relayService;

  /** Wires the handler with the session registry and relay service. */
  public DesktopWebSocketHandler(SessionRegistry sessionRegistry, RelayService relayService) {
    this.sessionRegistry = sessionRegistry;
    this.relayService = relayService;
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    String sessionId = UUID.randomUUID().toString();
    Sinks.Many<String> outbound = sessionRegistry.register(sessionId);
    LOGGER.debug("Desktop client connected: {}", sessionId);

    Mono<Void> send =
        session
            .send(outbound.asFlux().map(session::textMessage))
            .onErrorResume(error -> Mono.empty());

    Mono<Void> receive =
        session
            .receive()
            .filter(message -> message.getType() == WebSocketMessage.Type.TEXT)
            .flatMap(
                message ->
                    relayService
                        .ingest(message.getPayloadAsText())
                        .onErrorResume(error -> Mono.empty()))
            .then();

    return Mono.when(send, receive).doFinally(signal -> sessionRegistry.unregister(sessionId));
  }
}
