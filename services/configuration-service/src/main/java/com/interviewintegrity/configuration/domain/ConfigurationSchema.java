package com.interviewintegrity.configuration.domain;

import com.interviewintegrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Global catalog entry declaring a configuration key, its type and constraints. */
@Table("configuration_schema")
public class ConfigurationSchema implements Persistable<UUID> {

  @Id private UUID id;

  private String key;

  @Column("value_type")
  private ConfigValueType valueType;

  @Column("default_value")
  private String defaultValue;

  private Json constraints;

  private String description;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Declares a new configuration key in the global catalog. */
  public ConfigurationSchema(
      String key,
      ConfigValueType valueType,
      String defaultValue,
      String constraints,
      String description) {
    Assert.notBlank(key, "key");
    Assert.notNull(valueType, "valueType");
    this.key = key;
    this.valueType = valueType;
    this.defaultValue = defaultValue == null ? "{}" : defaultValue;
    this.constraints = Json.of(constraints == null ? "{}" : constraints);
    this.description = description;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected ConfigurationSchema() {}

  /** Updates the declaration of the configuration key. */
  public void update(
      ConfigValueType valueType, String defaultValue, String constraints, String description) {
    Assert.notNull(valueType, "valueType");
    this.valueType = valueType;
    this.defaultValue = defaultValue == null ? "{}" : defaultValue;
    this.constraints = Json.of(constraints == null ? "{}" : constraints);
    this.description = description;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public ConfigValueType getValueType() {
    return valueType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getConstraints() {
    return constraints == null ? "{}" : constraints.asString();
  }

  public String getDescription() {
    return description;
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
