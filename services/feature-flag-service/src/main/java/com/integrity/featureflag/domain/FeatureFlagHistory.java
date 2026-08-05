package com.integrity.featureflag.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Snapshot of a feature flag configuration, populated by a database trigger. */
@Table("feature_flags_history")
public class FeatureFlagHistory implements Persistable<Long> {

  @Id private Long historyId;

  @Column("history_action")
  private String historyAction;

  @Column("changed_by")
  private UUID changedBy;

  @Column("changed_at")
  private Instant changedAt;

  private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("feature_id")
  private UUID featureId;

  private String environment;
  private boolean enabled;

  @Column("rollout_percent")
  private int rolloutPercent;

  @Column("default_variant")
  private String defaultVariant;

  private String variants;
  private String rules;
  @Version private long version;

  /** Creates a feature flag history snapshot. */
  public FeatureFlagHistory(
      String historyAction,
      UUID changedBy,
      Instant changedAt,
      UUID id,
      UUID organizationId,
      UUID featureId,
      String environment,
      boolean enabled,
      int rolloutPercent,
      String defaultVariant,
      String variants,
      String rules,
      long version) {
    Assert.notBlank(historyAction, "historyAction");
    this.historyAction = historyAction;
    this.changedBy = changedBy;
    this.changedAt = changedAt;
    this.id = id;
    this.organizationId = organizationId;
    this.featureId = featureId;
    this.environment = environment;
    this.enabled = enabled;
    this.rolloutPercent = rolloutPercent;
    this.defaultVariant = defaultVariant;
    this.variants = variants;
    this.rules = rules;
    this.version = version;
  }

  protected FeatureFlagHistory() {}

  public Long getHistoryId() {
    return historyId;
  }

  /** Returns the flag UUID stored in this history snapshot. */
  public UUID getFlagId() {
    return id;
  }

  @Override
  public Long getId() {
    return historyId;
  }

  public String getHistoryAction() {
    return historyAction;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getFeatureId() {
    return featureId;
  }

  public String getEnvironment() {
    return environment;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getRolloutPercent() {
    return rolloutPercent;
  }

  public String getDefaultVariant() {
    return defaultVariant;
  }

  public String getVariants() {
    return variants;
  }

  public String getRules() {
    return rules;
  }

  public long getVersion() {
    return version;
  }

  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  @Override
  public boolean isNew() {
    return this.historyId == null;
  }
}
