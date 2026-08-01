package com.interviewintegrity.candidate.domain;

import com.interviewintegrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Tenant scoped tag that can be applied to candidates. */
@Table("tags")
public class Tag {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String code;

  private String name;

  @Column("created_at")
  private Instant createdAt;

  @Version private long version = 1;

  /** Creates a tag within the given tenant. */
  public Tag(UUID organizationId, String code, String name) {
    Assert.notBlank(code, "code");
    Assert.notBlank(name, "name");
    this.organizationId = organizationId;
    this.code = code;
    this.name = name;
    this.createdAt = Instant.now();
  }

  protected Tag() {}

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(UUID id) {
    this.id = id;
  }
}
