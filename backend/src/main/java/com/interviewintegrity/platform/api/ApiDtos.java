package com.interviewintegrity.platform.api;

import com.interviewintegrity.platform.domain.DomainModel.InterviewStatus;
import com.interviewintegrity.platform.domain.DomainModel.TelemetryType;
import com.interviewintegrity.platform.domain.DomainModel.ViolationSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class ApiDtos {
  private ApiDtos() {}

  public record AuthRequest(
      @NotBlank String email, @NotBlank String password, @NotBlank String deviceId) {}

  public record AuthResponse(String accessToken, String refreshToken, Instant expiresAt) {}

  public record InterviewCreateRequest(
      UUID candidateId, UUID recruiterId, String meetingUrl, Instant startsAt) {}

  public record InterviewResponse(
      UUID id, UUID candidateId, UUID recruiterId, String meetingUrl, InterviewStatus status) {}

  public record SessionStartRequest(
      @NotNull UUID interviewId, @NotBlank String deviceId, Map<String, Object> deviceSummary) {}

  public record SessionResponse(UUID id, UUID interviewId, String status, Instant startedAt) {}

  public record TelemetryIngestRequest(
      @NotNull UUID sessionId,
      @NotNull TelemetryType type,
      Instant occurredAt,
      Map<String, Object> payload) {}

  public record PolicyRule(
      String code, boolean enabled, ViolationSeverity severity, Map<String, Object> parameters) {}

  public record PolicyResponse(UUID id, String name, List<PolicyRule> rules, boolean enabled) {}

  public record ViolationResponse(
      UUID id,
      UUID sessionId,
      String ruleCode,
      ViolationSeverity severity,
      String message,
      Instant occurredAt) {}

  public record ReportResponse(
      UUID sessionId,
      int integrityScore,
      List<ViolationResponse> violations,
      Map<String, Object> deviceSummary) {}

  public record NotificationRequest(UUID userId, String channel, String subject, String body) {}
}
