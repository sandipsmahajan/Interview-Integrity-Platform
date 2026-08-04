package com.interviewintegrity.featureflag.domain;

import com.interviewintegrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Flag configuration for a feature within an environment. */
@Table("feature_flags")
public class FeatureFlag implements Persistable<UUID> {

  @Id private UUID id;

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

  private Json variants;
  private Json rules;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a flag configuration for a feature and environment. */
  public FeatureFlag(
      UUID organizationId,
      UUID featureId,
      String environment,
      boolean enabled,
      int rolloutPercent,
      String defaultVariant,
      String variants,
      String rules,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(featureId, "featureId");
    Assert.notBlank(environment, "environment");
    Assert.isTrue(
        rolloutPercent >= 0 && rolloutPercent <= 100, "rolloutPercent must be between 0 and 100");
    this.organizationId = organizationId;
    this.featureId = featureId;
    this.environment = environment;
    this.enabled = enabled;
    this.rolloutPercent = rolloutPercent;
    this.defaultVariant = defaultVariant;
    this.variants = Json.of(variants == null ? "{}" : variants);
    this.rules = Json.of(rules == null ? "{}" : rules);
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected FeatureFlag() {}

  /** Replaces the rollout configuration of the flag. */
  public void updateConfiguration(
      boolean enabled,
      int rolloutPercent,
      String defaultVariant,
      String variants,
      String rules,
      UUID byUser) {
    Assert.isTrue(
        rolloutPercent >= 0 && rolloutPercent <= 100, "rolloutPercent must be between 0 and 100");
    this.enabled = enabled;
    this.rolloutPercent = rolloutPercent;
    this.defaultVariant = defaultVariant;
    this.variants = Json.of(variants == null ? "{}" : variants);
    this.rules = Json.of(rules == null ? "{}" : rules);
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
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
    return variants == null ? "{}" : variants.asString();
  }

  public String getRules() {
    return rules == null ? "{}" : rules.asString();
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
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
