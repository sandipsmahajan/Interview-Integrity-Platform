package com.integrity.interview.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Mirror of an external calendar provider event for an interview. */
@Table("interview_calendar_events")
public class InterviewCalendarEvent implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("interview_id")
  private UUID interviewId;

  private String provider;

  @Column("provider_event_id")
  private String providerEventId;

  @Column("event_url")
  private String eventUrl;

  @Column("starts_at")
  private Instant startsAt;

  @Column("ends_at")
  private Instant endsAt;

  private String status;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a confirmed calendar event mirror. */
  public InterviewCalendarEvent(
      UUID organizationId,
      UUID interviewId,
      String provider,
      String providerEventId,
      String eventUrl,
      Instant startsAt,
      Instant endsAt) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(interviewId, "interviewId");
    Assert.notBlank(provider, "provider");
    Assert.notBlank(providerEventId, "providerEventId");
    Assert.notNull(startsAt, "startsAt");
    Assert.notNull(endsAt, "endsAt");
    Assert.isTrue(endsAt.isAfter(startsAt), "endsAt must be after startsAt");
    this.organizationId = organizationId;
    this.interviewId = interviewId;
    this.provider = provider;
    this.providerEventId = providerEventId;
    this.eventUrl = eventUrl;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.status = "CONFIRMED";
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected InterviewCalendarEvent() {}

  /** Updates the event mirror details. */
  public void update(String eventUrl, Instant startsAt, Instant endsAt, String status) {
    Assert.notNull(startsAt, "startsAt");
    Assert.notNull(endsAt, "endsAt");
    Assert.isTrue(endsAt.isAfter(startsAt), "endsAt must be after startsAt");
    this.eventUrl = eventUrl;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    if (status != null) {
      this.status = status;
    }
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getInterviewId() {
    return interviewId;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderEventId() {
    return providerEventId;
  }

  public String getEventUrl() {
    return eventUrl;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
