package com.interviewintegrity.platform.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
public class ReactiveErrorConfiguration {
  @Bean
  @Order(-2)
  WebExceptionHandler reactiveApiErrorHandler(ObjectMapper objectMapper) {
    return (exchange, exception) -> writeError(exchange, objectMapper, exception);
  }

  private static Mono<Void> writeError(
      ServerWebExchange exchange, ObjectMapper objectMapper, Throwable exception) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(exception);
    }
    HttpStatus status = statusFor(exception);
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] payload = serialize(objectMapper, Map.of(
        "error", status == HttpStatus.BAD_REQUEST ? "bad_request" : "internal_server_error",
        "message", status == HttpStatus.BAD_REQUEST ? exception.getMessage() : "Request could not be processed"));
    return exchange.getResponse()
        .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
  }

  private static HttpStatus statusFor(Throwable exception) {
    return exception instanceof IllegalArgumentException || exception instanceof ValidationException
        ? HttpStatus.BAD_REQUEST
        : HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private static byte[] serialize(ObjectMapper objectMapper, Map<String, String> body) {
    try {
      return objectMapper.writeValueAsBytes(body);
    } catch (JsonProcessingException exception) {
      return "{\"error\":\"internal_server_error\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
  }
}
