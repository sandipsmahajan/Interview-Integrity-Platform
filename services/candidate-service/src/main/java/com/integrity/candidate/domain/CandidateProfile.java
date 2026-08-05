package com.integrity.candidate.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** One-to-one extended profile of a candidate. */
@Table("candidate_profiles")
public class CandidateProfile implements Persistable<UUID> {

  @Id private UUID id;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("organization_id")
  private UUID organizationId;

  private String headline;

  private String bio;

  private String location;

  private String timezone;

  @Column("resume_summary")
  private String resumeSummary;

  @Column("linkedin_url")
  private String linkedinUrl;

  @Column("github_url")
  private String githubUrl;

  private String[] skills;

  @Column("experience_years")
  private BigDecimal experienceYears;

  private Json attributes;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  @Override
  public boolean isNew() {
    return this.id == null;
  }

  /** Creates an empty extended profile for a candidate. */
  public CandidateProfile(UUID candidateId, UUID organizationId) {
    this.candidateId = candidateId;
    this.organizationId = organizationId;
    this.skills = new String[0];
    this.attributes = Json.of("{}");
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected CandidateProfile() {}

  /** Replaces the extended profile details. */
  public void update(
      String headline,
      String bio,
      String location,
      String timezone,
      String resumeSummary,
      String linkedinUrl,
      String githubUrl,
      List<String> skills,
      BigDecimal experienceYears,
      String attributes) {
    Assert.isTrue(
        linkedinUrl == null || linkedinUrl.startsWith("http"),
        "linkedinUrl must be an http(s) URL");
    Assert.isTrue(
        githubUrl == null || githubUrl.startsWith("http"), "githubUrl must be an http(s) URL");
    Assert.isTrue(
        experienceYears == null
            || (experienceYears.compareTo(BigDecimal.ZERO) >= 0
                && experienceYears.compareTo(BigDecimal.valueOf(60)) <= 0),
        "experienceYears must be between 0 and 60");
    this.headline = headline;
    this.bio = bio;
    this.location = location;
    this.timezone = timezone;
    this.resumeSummary = resumeSummary;
    this.linkedinUrl = linkedinUrl;
    this.githubUrl = githubUrl;
    this.skills = skills == null ? new String[0] : skills.toArray(new String[0]);
    this.experienceYears = experienceYears;
    this.attributes = Json.of(attributes == null ? "{}" : attributes);
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getHeadline() {
    return headline;
  }

  public String getBio() {
    return bio;
  }

  public String getLocation() {
    return location;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getResumeSummary() {
    return resumeSummary;
  }

  public String getLinkedinUrl() {
    return linkedinUrl;
  }

  public String getGithubUrl() {
    return githubUrl;
  }

  public String[] getSkills() {
    return skills == null ? new String[0] : skills.clone();
  }

  public BigDecimal getExperienceYears() {
    return experienceYears;
  }

  public String getAttributes() {
    return attributes == null ? "{}" : attributes.asString();
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
}
