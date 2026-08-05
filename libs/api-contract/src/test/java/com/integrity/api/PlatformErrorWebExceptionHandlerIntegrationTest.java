package com.integrity.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.integrity.exception.AuthenticationFailedException;
import com.integrity.exception.ValidationFailedException;
import com.integrity.exception.Violation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Boots the real auto-configuration (including {@code ErrorWebFluxAutoConfiguration}) to prove the
 * platform error handler wins over Spring Boot's default handler and renders the platform error
 * contract.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = PlatformErrorWebExceptionHandlerIntegrationTest.IntegrationConfig.class)
class PlatformErrorWebExceptionHandlerIntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  static class IntegrationConfig {

    @RestController
    static class ThrowingController {

      @GetMapping("/fail-auth")
      Mono<Void> failAuth() {
        return Mono.error(new AuthenticationFailedException("Invalid credentials"));
      }

      @GetMapping("/fail-validation")
      Mono<Void> failValidation() {
        return Mono.error(
            new ValidationFailedException(
                "bad input", List.of(new Violation("email", "must not be blank"))));
      }

      @GetMapping("/fail-internal")
      Mono<Void> failInternal() {
        return Mono.error(new IllegalStateException("boom"));
      }
    }
  }

  @Autowired ApplicationContext applicationContext;

  @LocalServerPort int port;

  WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void platformHandlerIsTheOnlyErrorWebExceptionHandlerBean() {
    String[] beanNames = applicationContext.getBeanNamesForType(ErrorWebExceptionHandler.class);

    assertThat(beanNames).containsExactly("platformErrorWebExceptionHandler");
  }

  @Test
  void authenticationFailureRenders401WithPlatformErrorBody() {
    webTestClient
        .get()
        .uri("/fail-auth")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.code")
        .isEqualTo("UNAUTHENTICATED")
        .jsonPath("$.message")
        .isEqualTo("Invalid credentials")
        .jsonPath("$.timestamp")
        .exists()
        .jsonPath("$.violations")
        .isArray();
  }

  @Test
  void validationFailureRenders400WithViolations() {
    webTestClient
        .get()
        .uri("/fail-validation")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.code")
        .isEqualTo("VALIDATION_FAILED")
        .jsonPath("$.violations[0].field")
        .isEqualTo("email")
        .jsonPath("$.violations[0].message")
        .isEqualTo("must not be blank");
  }

  @Test
  void unhandledExceptionRenders500WithGenericMessage() {
    webTestClient
        .get()
        .uri("/fail-internal")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(500)
        .jsonPath("$.code")
        .isEqualTo("INTERNAL_ERROR")
        .jsonPath("$.message")
        .isEqualTo(
            "An unexpected internal error occurred. Contact support with the trace id.");
  }
}
