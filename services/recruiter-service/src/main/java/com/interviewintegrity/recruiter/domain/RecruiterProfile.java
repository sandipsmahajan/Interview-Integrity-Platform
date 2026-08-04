package com.interviewintegrity.recruiter.domain;

import com.interviewintegrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** One-to-one extended profile of a recruiter. */
@Table("recruiter_profiles")
public class RecruiterProfile implements Persistable<UUID> {

  @Id private UUID id;

  @Column("recruiter_id")
  private UUID recruiterId;

  @Column("organization_id")
  private UUID organizationId;

  private String bio;

  private String[] specialties;

  @Column("linkedin_url")
  private String linkedinUrl;

  private Json availability;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates an empty extended profile for a recruiter. */
  public RecruiterProfile(UUID recruiterId, UUID organizationId) {
    this.recruiterId = recruiterId;
    this.organizationId = organizationId;
    this.specialties = new String[0];
    this.availability = Json.of("{}");
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected RecruiterProfile() {}

  /** Replaces the extended profile details. */
  public void update(String bio, String[] specialties, String linkedinUrl, String availability) {
    Assert.isTrue(
        linkedinUrl == null || linkedinUrl.startsWith("http"),
        "linkedinUrl must be an http(s) URL");
    this.bio = bio;
    this.specialties = specialties == null ? new String[0] : specialties.clone();
    this.linkedinUrl = linkedinUrl;
    this.availability = Json.of(availability == null ? "{}" : availability);
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getRecruiterId() {
    return recruiterId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getBio() {
    return bio;
  }

  public String[] getSpecialties() {
    return specialties == null ? new String[0] : specialties.clone();
  }

  public String getLinkedinUrl() {
    return linkedinUrl;
  }

  public String getAvailability() {
    return availability == null ? "{}" : availability.asString();
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
