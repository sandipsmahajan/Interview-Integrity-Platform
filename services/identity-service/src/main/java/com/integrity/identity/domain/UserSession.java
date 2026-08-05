package com.integrity.identity.domain;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A user session with its opaque refresh token stored as a SHA-256 hash. */
@Table("user_sessions")
public class UserSession implements Persistable<UUID> {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("organization_id")
  private UUID organizationId;

  @Column("refresh_token_hash")
  private String refreshTokenHash;

  @Column("device_id")
  private String deviceId;

  @Column("ip_address")
  private InetAddress ipAddress;

  @Column("user_agent")
  private String userAgent;

  private SessionStatus status;

  @Column("issued_at")
  private Instant issuedAt;

  @Column("expires_at")
  private Instant expiresAt;

  @Column("last_used_at")
  private Instant lastUsedAt;

  @Column("revoked_at")
  private Instant revokedAt;

  @Column("revoked_by")
  private UUID revokedBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a new active session. */
  public UserSession(
      UUID userId,
      UUID organizationId,
      String refreshTokenHash,
      String deviceId,
      InetAddress ipAddress,
      String userAgent,
      Instant expiresAt) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.refreshTokenHash = refreshTokenHash;
    this.deviceId = deviceId;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.status = SessionStatus.ACTIVE;
    Instant now = Instant.now();
    this.issuedAt = now;
    this.expiresAt = expiresAt;
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected UserSession() {}

  /** Marks the session as refreshed (superseded by a newer session). */
  public void markRefreshed() {
    this.status = SessionStatus.REFRESHED;
    this.updatedAt = Instant.now();
  }

  /** Revokes the session. */
  public void revoke(UUID byUser) {
    this.status = SessionStatus.REVOKED;
    this.revokedBy = byUser;
    this.revokedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Marks the session as expired. */
  public void expire() {
    this.status = SessionStatus.EXPIRED;
    this.updatedAt = Instant.now();
  }

  /** Touches the session usage timestamp. */
  public void touch() {
    this.lastUsedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Returns true when the session is still active and not expired. */
  public boolean isUsable() {
    return status == SessionStatus.ACTIVE && expiresAt.isAfter(Instant.now());
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getRefreshTokenHash() {
    return refreshTokenHash;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public InetAddress getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public UUID getRevokedBy() {
    return revokedBy;
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
