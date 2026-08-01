package com.interviewintegrity.candidate.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Data-protection consent granted by a candidate. */
@Table("candidate_consents")
public class CandidateConsent {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("candidate_id")
  private UUID candidateId;

  @Column("consent_type")
  private String consentType;

  private ConsentStatus status;

  @Column("granted_at")
  private Instant grantedAt;

  @Column("granted_by")
  private UUID grantedBy;

  @Column("revoked_at")
  private Instant revokedAt;

  @Column("revoked_by")
  private UUID revokedBy;

  @Column("consent_version")
  private String consentVersion;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Grants a consent to a candidate. */
  public CandidateConsent(
      UUID organizationId,
      UUID candidateId,
      String consentType,
      String consentVersion,
      UUID grantedBy) {
    Assert.notBlank(consentType, "consentType");
    this.organizationId = organizationId;
    this.candidateId = candidateId;
    this.consentType = consentType;
    this.grantedBy = grantedBy;
    this.consentVersion = consentVersion == null ? "1.0" : consentVersion;
    this.status = ConsentStatus.GRANTED;
    Instant now = Instant.now();
    this.grantedAt = now;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected CandidateConsent() {}

  /** Withdraws the consent. */
  public void revoke(UUID revokedBy) {
    this.status = ConsentStatus.REVOKED;
    this.revokedBy = revokedBy;
    this.revokedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getCandidateId() {
    return candidateId;
  }

  public String getConsentType() {
    return consentType;
  }

  public ConsentStatus getStatus() {
    return status;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public UUID getGrantedBy() {
    return grantedBy;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public UUID getRevokedBy() {
    return revokedBy;
  }

  public String getConsentVersion() {
    return consentVersion;
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
