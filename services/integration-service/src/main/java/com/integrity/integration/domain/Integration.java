package com.integrity.integration.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** An integration definition per tenant. */
@Table("integrations")
public class Integration implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String provider;

  private String name;

  private IntegrationStatus status;

  @Column("credentials_ref")
  private String credentialsRef;

  private Json config;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_by")
  private UUID deletedBy;

  @Column("deleted_at")
  private Instant deletedAt;

  @Version private long version = 1;

  /** Creates a new disconnected integration. */
  public Integration(
      UUID organizationId,
      String provider,
      String name,
      String credentialsRef,
      String config,
      UUID createdBy) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notBlank(provider, "provider");
    Assert.notBlank(name, "name");
    Assert.notBlank(credentialsRef, "credentialsRef");
    this.organizationId = organizationId;
    this.provider = provider;
    this.name = name;
    this.credentialsRef = credentialsRef;
    this.config = Json.of(config);
    this.createdBy = createdBy;
    this.status = IntegrationStatus.DISCONNECTED;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Integration() {}

  /** Updates the display name and configuration. */
  public void update(String name, String config, UUID updatedBy) {
    Assert.notBlank(name, "name");
    this.name = name;
    this.config = Json.of(config);
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Marks the integration as connected. */
  public void connect(UUID updatedBy) {
    this.status = IntegrationStatus.CONNECTED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Marks the integration as disconnected. */
  public void disconnect(UUID updatedBy) {
    this.status = IntegrationStatus.DISCONNECTED;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Marks the integration as errored. */
  public void markError(UUID updatedBy) {
    this.status = IntegrationStatus.ERROR;
    this.updatedBy = updatedBy;
    this.updatedAt = Instant.now();
  }

  /** Marks the integration as soft deleted. */
  public void delete(UUID deletedBy) {
    this.deletedBy = deletedBy;
    this.deletedAt = Instant.now();
    this.updatedBy = deletedBy;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getProvider() {
    return provider;
  }

  public String getName() {
    return name;
  }

  public IntegrationStatus getStatus() {
    return status;
  }

  public String getCredentialsRef() {
    return credentialsRef;
  }

  public String getConfig() {
    return config == null ? "{}" : config.asString();
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

  public UUID getDeletedBy() {
    return deletedBy;
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
