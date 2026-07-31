package com.interviewintegrity.platform.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class DomainModel {
  private DomainModel() {}

  public enum InterviewStatus {
    SCHEDULED,
    READY,
    LIVE,
    COMPLETED,
    CANCELLED
  }

  public enum SessionStatus {
    CREATED,
    AUTHENTICATED,
    ACTIVE,
    DEGRADED,
    ENDED
  }

  public enum ViolationSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public enum TelemetryType {
    HEARTBEAT,
    DEVICE,
    DISPLAY,
    WINDOW_FOCUS,
    PROCESS,
    NETWORK,
    AUDIO,
    VIDEO,
    BROWSER,
    CRASH
  }

  @Table(name = "companies")
  public static class Company {
    @Id public UUID id;

    @Column("name")
    public String name;

    @Column("slug")
    public String slug;

    @Column("active")
    public boolean active = true;
  }

  @Table(name = "users")
  public static class User {
    @Id public UUID id;

    @Column("company_id")
    public UUID companyId;

    @Column("email")
    public String email;

    @Column("display_name")
    public String displayName;
  }

  @Table(name = "interviews")
  public static class Interview {
    @Id public UUID id;

    @Column("company_id")
    public UUID companyId;

    @Column("candidate_id")
    public UUID candidateId;

    @Column("recruiter_id")
    public UUID recruiterId;

    @Column("meeting_url")
    public String meetingUrl;

    @Column("status")
    public InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column("starts_at")
    public Instant startsAt;

    @Column("ends_at")
    public Instant endsAt;
  }

  @Table(name = "interview_sessions")
  public static class InterviewSession {
    @Id public UUID id;

    @Column("interview_id")
    public UUID interviewId;

    @Column("candidate_id")
    public UUID candidateId;

    @Column("device_id")
    public String deviceId;

    @Column("status")
    public SessionStatus status = SessionStatus.CREATED;

    @Column("started_at")
    public Instant startedAt = Instant.now();

    @Column("last_heartbeat_at")
    public Instant lastHeartbeatAt;
  }

  @Table(name = "telemetry_events")
  public static class TelemetryEvent {
    @Id public UUID id;

    @Column("session_id")
    public UUID sessionId;

    @Column("type")
    public TelemetryType type;

    @Column("payload_json")
    public String payloadJson;

    @Column("occurred_at")
    public Instant occurredAt;
  }

  @Table(name = "policies")
  public static class Policy {
    @Id public UUID id;

    @Column("company_id")
    public UUID companyId;

    @Column("name")
    public String name;

    @Column("rules_json")
    public String rulesJson;

    @Column("enabled")
    public boolean enabled = true;
  }

  @Table(name = "violations")
  public static class Violation {
    @Id public UUID id;

    @Column("session_id")
    public UUID sessionId;

    @Column("rule_code")
    public String ruleCode;

    @Column("severity")
    public ViolationSeverity severity;

    @Column("message")
    public String message;

    @Column("occurred_at")
    public Instant occurredAt = Instant.now();
  }
}
