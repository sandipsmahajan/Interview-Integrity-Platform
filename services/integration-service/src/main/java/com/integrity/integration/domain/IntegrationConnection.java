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

/** A live connection to an external account under an integration. */
@Table("integration_connections")
public class IntegrationConnection implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("integration_id")
  private UUID integrationId;

  @Column("external_account_id")
  private String externalAccountId;

  private IntegrationStatus status;

  private String[] scopes;

  @Column("connected_at")
  private Instant connectedAt;

  @Column("last_sync_at")
  private Instant lastSyncAt;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a new disconnected connection for an external account. */
  public IntegrationConnection(UUID organizationId, UUID integrationId, String externalAccountId) {
    Assert.notNull(organizationId, "organizationId");
    Assert.notNull(integrationId, "integrationId");
    Assert.notBlank(externalAccountId, "externalAccountId");
    this.organizationId = organizationId;
    this.integrationId = integrationId;
    this.externalAccountId = externalAccountId;
    this.status = IntegrationStatus.DISCONNECTED;
    this.scopes = new String[0];
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected IntegrationConnection() {}

  /** Establishes the connection with the granted scopes. */
  public void connect(List<String> scopes) {
    this.status = IntegrationStatus.CONNECTED;
    if (this.connectedAt == null) {
      this.connectedAt = Instant.now();
    }
    this.scopes = scopes == null ? new String[0] : scopes.toArray(new String[0]);
    this.updatedAt = Instant.now();
  }

  /** Marks the connection as disconnected. */
  public void disconnect() {
    this.status = IntegrationStatus.DISCONNECTED;
    this.updatedAt = Instant.now();
  }

  /** Marks the connection as errored. */
  public void markError() {
    this.status = IntegrationStatus.ERROR;
    this.updatedAt = Instant.now();
  }

  /** Records the completion of a synchronization run. */
  public void recordSync() {
    this.lastSyncAt = Instant.now();
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

  public String getExternalAccountId() {
    return externalAccountId;
  }

  public IntegrationStatus getStatus() {
    return status;
  }

  public List<String> getScopes() {
    return Arrays.asList(scopes);
  }

  public Instant getConnectedAt() {
    return connectedAt;
  }

  public Instant getLastSyncAt() {
    return lastSyncAt;
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
