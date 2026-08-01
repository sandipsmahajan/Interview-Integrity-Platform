package com.interviewintegrity.event;

/** Well-known Kafka topic names used by the platform's event bus. */
public final class KafkaTopics {
  private KafkaTopics() {}

  public static final String IDENTITY_USER_REGISTERED = "identity.user-registered.v1";
  public static final String IDENTITY_USER_UPDATED = "identity.user-updated.v1";
  public static final String ORGANIZATION_REGISTERED = "organization.registered.v1";
  public static final String INTERVIEW_CREATED = "interview.created.v1";
  public static final String INTERVIEW_SCHEDULED = "interview.scheduled.v1";
  public static final String INTERVIEW_STARTED = "interview.started.v1";
  public static final String INTERVIEW_COMPLETED = "interview.completed.v1";
  public static final String TELEMETRY_RECEIVED = "telemetry.received.v1";
  public static final String POLICY_VIOLATION = "policy.violation.v1";
  public static final String REPORT_GENERATED = "report.generated.v1";
  public static final String IDENTITY_EMAIL = "identity.email.v1";
}
