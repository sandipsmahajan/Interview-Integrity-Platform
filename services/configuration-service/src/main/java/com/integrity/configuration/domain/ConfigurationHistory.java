package com.integrity.configuration.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

/** Immutable version record of a configuration change, populated by a database trigger. */
@Table("configuration_history")
public class ConfigurationHistory implements Persistable<Long> {

  @Id private Long id;

  @Column("configuration_id")
  private UUID configurationId;

  @Column("organization_id")
  private UUID organizationId;

  private String key;

  @Column("old_value")
  private String oldValue;

  @Column("new_value")
  private String newValue;

  @Column("changed_by")
  private UUID changedBy;

  @Column("changed_at")
  private Instant changedAt;

  private long version;

  /** Creates a configuration history record. */
  public ConfigurationHistory(
      UUID configurationId,
      UUID organizationId,
      String key,
      String oldValue,
      String newValue,
      UUID changedBy,
      Instant changedAt,
      long version) {
    Assert.notNull(configurationId, "configurationId");
    Assert.notBlank(key, "key");
    this.configurationId = configurationId;
    this.organizationId = organizationId;
    this.key = key;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
    this.version = version;
  }

  protected ConfigurationHistory() {}

  @Override
  public Long getId() {
    return id;
  }

  public UUID getConfigurationId() {
    return configurationId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getKey() {
    return key;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public long getVersion() {
    return version;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return this.id == null;
  }
}
