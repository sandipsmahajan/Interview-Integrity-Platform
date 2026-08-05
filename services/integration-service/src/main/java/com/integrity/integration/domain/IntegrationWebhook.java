package com.integrity.integration.domain;

import com.integrity.validation.Assert;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** An outbound webhook subscription for an integration. */
@Table("integration_webhooks")
public class IntegrationWebhook implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("integration_id")
  private UUID integrationId;

  private String url;

  @Column("secret_hash")
  private String secretHash;

  private String[] events;

  private boolean enabled;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a new enabled webhook. */
  public IntegrationWebhook(
      UUID organizationId, UUID integrationId, String url, String secretHash, List<String> events) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(integrationId, "integrationId");
    Assert.notBlank(url, "url");
    Assert.notBlank(secretHash, "secretHash");
    this.organizationId = organizationId;
    this.integrationId = integrationId;
    this.url = url;
    this.secretHash = secretHash;
    this.events = events == null ? new String[0] : events.toArray(new String[0]);
    this.enabled = true;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected IntegrationWebhook() {}

  /** Updates the delivery url and subscribed events. */
  public void update(String url, List<String> events) {
    Assert.notBlank(url, "url");
    this.url = url;
    this.events = events == null ? new String[0] : events.toArray(new String[0]);
    this.updatedAt = Instant.now();
  }

  /** Enables delivery of events. */
  public void enable() {
    this.enabled = true;
    this.updatedAt = Instant.now();
  }

  /** Disables delivery of events. */
  public void disable() {
    this.enabled = false;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getIntegrationId() {
    return integrationId;
  }

  public String getUrl() {
    return url;
  }

  public String getSecretHash() {
    return secretHash;
  }

  public List<String> getEvents() {
    return Arrays.asList(events);
  }

  public boolean isEnabled() {
    return enabled;
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
