package com.interviewintegrity.desktopclient.config;

import com.interviewintegrity.desktopclient.service.KafkaRelayConsumer;
import com.interviewintegrity.desktopclient.service.RelayService;
import com.interviewintegrity.desktopclient.service.SessionRegistry;
import com.interviewintegrity.desktopclient.web.DesktopWebSocketHandler;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * Explicit bean wiring for the desktop client relay.
 *
 * <p>The WebSocket handler is exposed at {@code /ws/desktop}; the Kafka relay consumer is started
 * when the application context starts.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the in-memory session registry. */
  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistry();
  }

  /** Provides the relay service bridging WebSocket and Kafka traffic. */
  @Bean
  public RelayService relayService(
      reactor.kafka.sender.KafkaSender<String, String> sender, SessionRegistry sessionRegistry) {
    return new RelayService(sender, sessionRegistry);
  }

  /** Starts the Kafka relay consumer at startup. */
  @Bean
  public KafkaRelayConsumer kafkaRelayConsumer(
      reactor.kafka.receiver.KafkaReceiver<String, String> receiver, RelayService relayService) {
    KafkaRelayConsumer consumer = new KafkaRelayConsumer(receiver, relayService);
    consumer.start();
    return consumer;
  }

  /** Exposes the desktop WebSocket handler. */
  @Bean
  public WebSocketHandler desktopWebSocketHandler(
      SessionRegistry sessionRegistry, RelayService relayService) {
    return new DesktopWebSocketHandler(sessionRegistry, relayService);
  }

  /** Maps the WebSocket endpoint URL to the handler. */
  @Bean
  public HandlerMapping webSocketHandlerMapping(WebSocketHandler desktopWebSocketHandler) {
    Map<String, WebSocketHandler> urlMap = Map.of("/ws/desktop", desktopWebSocketHandler);
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    mapping.setUrlMap(urlMap);
    mapping.setOrder(-1);
    return mapping;
  }

  /** Adapts WebSocket sessions in the reactive stack. */
  @Bean
  public WebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new WebSocketHandlerAdapter();
  }

  /** Describes the OpenAPI document for the desktop client service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Desktop Client Service API")
                .version("v1")
                .description("WebSocket session relay bridging desktop clients and the platform"));
  }
}
