package com.interviewintegrity.platform.infrastructure;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Flux;

@Configuration(proxyBeanMethods = false)
public class WebSocketConfiguration {
  @Bean
  WebSocketHandler recruiterTelemetrySocketHandler() {
    return session -> {
      Flux<String> stream = Flux.interval(Duration.ofSeconds(5))
          .onBackpressureLatest()
          .map(sequence -> "{\"type\":\"heartbeat\",\"sequence\":" + sequence + "}");
      return session.send(stream.map(session::textMessage));
    };
  }

  @Bean
  HandlerMapping webSocketHandlerMapping(WebSocketHandler recruiterTelemetrySocketHandler) {
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    mapping.setOrder(-1);
    mapping.setUrlMap(java.util.Map.of("/ws/recruiter/telemetry", recruiterTelemetrySocketHandler));
    return mapping;
  }
}
