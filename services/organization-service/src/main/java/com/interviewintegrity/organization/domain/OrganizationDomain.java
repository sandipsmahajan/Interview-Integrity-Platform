package com.interviewintegrity.organization.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Email domain claimed by a tenant, used for SSO and auto provisioning. */
@Table("organization_domains")
public class OrganizationDomain implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String domain;

  @Column("verified_at")
  private Instant verifiedAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates a new unverified domain claim. */
  public OrganizationDomain(UUID organizationId, String domain, UUID createdBy) {
    Assert.notBlank(domain, "domain");
    this.organizationId = organizationId;
    this.domain = domain.toLowerCase(Locale.ROOT);
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected OrganizationDomain() {}

  /** Marks the domain as verified. */
  public void verify() {
    this.verifiedAt = verifiedAt == null ? Instant.now() : verifiedAt;
    this.updatedAt = Instant.now();
  }

  /** Marks the domain claim as soft deleted. */
  public void delete() {
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getDomain() {
    return domain;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
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
