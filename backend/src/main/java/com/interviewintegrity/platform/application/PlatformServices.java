package com.interviewintegrity.platform.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewintegrity.platform.api.ApiDtos.AuthResponse;
import com.interviewintegrity.platform.api.ApiDtos.ReportResponse;
import com.interviewintegrity.platform.api.ApiDtos.TelemetryIngestRequest;
import com.interviewintegrity.platform.api.ApiDtos.ViolationResponse;
import com.interviewintegrity.platform.domain.DomainModel.TelemetryEvent;
import com.interviewintegrity.platform.domain.DomainModel.TelemetryType;
import com.interviewintegrity.platform.domain.DomainModel.Violation;
import com.interviewintegrity.platform.domain.DomainModel.ViolationSeverity;
import com.interviewintegrity.platform.infrastructure.Repositories.TelemetryEventRepository;
import com.interviewintegrity.platform.infrastructure.Repositories.ViolationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class PlatformServices {
  private PlatformServices() {}

  public interface TelemetryCommandService {
    Mono<List<ViolationResponse>> ingest(TelemetryIngestRequest request);
  }

  public interface PolicyEvaluationService {
    Flux<Violation> evaluate(TelemetryIngestRequest request);
  }

  public interface ReportService {
    Mono<ReportResponse> buildSessionReport(UUID sessionId);
  }

  public interface NotificationService {
    Mono<Void> send(UUID userId, String channel, String subject, String body);
  }

  public interface TokenIssuer {
    Mono<AuthResponse> issue(String subject, String deviceId);
  }

  public interface ReactiveEventPublisher {
    Mono<Void> publish(String topic, String key, String payload);
  }

  public static class HmacTokenIssuer implements TokenIssuer {
    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public HmacTokenIssuer(String secret, ObjectMapper objectMapper) {
      this.secret = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      this.objectMapper = objectMapper;
    }

    @Override
    public Mono<AuthResponse> issue(String subject, String deviceId) {
      return Mono.fromSupplier(() -> {
        Instant expiresAt = Instant.now().plusSeconds(900);
        String accessToken = sign(Map.of("sub", subject, "deviceId", deviceId, "exp", expiresAt.getEpochSecond()));
        String refreshToken = sign(Map.of("sub", subject, "deviceId", deviceId, "type", "refresh", "iat", Instant.now().getEpochSecond()));
        return new AuthResponse(accessToken, refreshToken, expiresAt);
      });
    }

    private String sign(Map<String, Object> claims) {
      try {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String payload = base64Url(objectMapper.writeValueAsBytes(claims));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        String signature = base64Url(mac.doFinal((header + "." + payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return header + "." + payload + "." + signature;
      } catch (Exception e) {
        throw new IllegalStateException("Unable to issue token", e);
      }
    }

    private static String base64Url(byte[] bytes) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
  }

  public static class DefaultPolicyEvaluationService implements PolicyEvaluationService {
    @Override
    public Flux<Violation> evaluate(TelemetryIngestRequest request) {
      if (request.type() == TelemetryType.HEARTBEAT) {
        Object interval = request.payload().get("secondsSincePreviousHeartbeat");
        if (interval instanceof Number number && number.longValue() > Duration.ofSeconds(10).toSeconds()) {
          return Flux.just(violation(request.sessionId(), "HEARTBEAT_STALE", ViolationSeverity.HIGH,
              "Heartbeat exceeded the configured 5 second cadence."));
        }
      }
      if (Boolean.TRUE.equals(request.payload().get("virtualMachineDetected"))) {
        return Flux.just(violation(request.sessionId(), "VM_DETECTED", ViolationSeverity.CRITICAL,
            "A virtual machine indicator was reported by the consented system check."));
      }
      if (Boolean.TRUE.equals(request.payload().get("browserOutOfFocus"))) {
        return Flux.just(violation(request.sessionId(), "BROWSER_FOCUS_LOST", ViolationSeverity.MEDIUM,
            "The secured interview browser lost focus."));
      }
      return Flux.empty();
    }

    private static Violation violation(UUID sessionId, String code, ViolationSeverity severity, String message) {
      Violation violation = new Violation();
      violation.id = UUID.randomUUID();
      violation.sessionId = sessionId;
      violation.ruleCode = code;
      violation.severity = severity;
      violation.message = message;
      return violation;
    }
  }

  public static class DefaultTelemetryCommandService implements TelemetryCommandService {
    private final TelemetryEventRepository events;
    private final ViolationRepository violations;
    private final PolicyEvaluationService policy;
    private final ObjectMapper objectMapper;
    private final ReactiveEventPublisher publisher;

    public DefaultTelemetryCommandService(
        TelemetryEventRepository events,
        ViolationRepository violations,
        PolicyEvaluationService policy,
        ObjectMapper objectMapper,
        ReactiveEventPublisher publisher) {
      this.events = events;
      this.violations = violations;
      this.policy = policy;
      this.objectMapper = objectMapper;
      this.publisher = publisher;
    }

    @Override
    public Mono<List<ViolationResponse>> ingest(TelemetryIngestRequest request) {
      TelemetryEvent event = new TelemetryEvent();
      event.id = UUID.randomUUID();
      event.sessionId = request.sessionId();
      event.type = request.type();
      event.occurredAt = request.occurredAt() == null ? Instant.now() : request.occurredAt();
      event.payloadJson = toJson(request.payload());

      return events.save(event)
          .thenMany(violations.saveAll(policy.evaluate(request)))
          .flatMap(violation -> publisher.publish("policy.violations", request.sessionId().toString(), violation.ruleCode)
              .thenReturn(violation))
          .map(PlatformServices::toResponse)
          .collectList()
          .flatMap(detected -> publisher.publish("telemetry.events", request.sessionId().toString(), event.payloadJson)
              .thenReturn(detected));
    }

    private String toJson(Map<String, Object> payload) {
      try {
        return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Telemetry payload is not serializable", e);
      }
    }
  }

  public static class DefaultReportService implements ReportService {
    private final ViolationRepository violations;

    public DefaultReportService(ViolationRepository violations) {
      this.violations = violations;
    }

    @Override
    public Mono<ReportResponse> buildSessionReport(UUID sessionId) {
      return violations.findBySessionIdOrderByOccurredAtAsc(sessionId)
          .map(PlatformServices::toResponse)
          .collectList()
          .map(items -> {
            int score = Math.max(0, 100 - items.stream().mapToInt(v -> switch (v.severity()) {
              case INFO, LOW -> 3;
              case MEDIUM -> 8;
              case HIGH -> 18;
              case CRITICAL -> 35;
            }).sum());
            return new ReportResponse(sessionId, score, items, Map.of());
          });
    }
  }

  public static ViolationResponse toResponse(Violation violation) {
    return new ViolationResponse(violation.id, violation.sessionId, violation.ruleCode,
        violation.severity, violation.message, violation.occurredAt);
  }
}
