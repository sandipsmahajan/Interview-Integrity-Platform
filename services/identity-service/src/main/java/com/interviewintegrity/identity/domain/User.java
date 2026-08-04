package com.interviewintegrity.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A platform user account, scoped to an organization. */
@Table("users")
public class User implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String email;

  @Column("password_hash")
  private String passwordHash;

  @Column("display_name")
  private String displayName;

  private UserStatus status;

  @Column("email_verified_at")
  private Instant emailVerifiedAt;

  @Column("last_login_at")
  private Instant lastLoginAt;

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

  @Column("failed_login_attempts")
  private int failedLoginAttempts;

  @Column("locked_until")
  private Instant lockedUntil;

  @Column("password_reset_requested_at")
  private Instant passwordResetRequestedAt;

  @Version private long version = 1;

  /** Creates a new pending user with the given profile. */
  public User(UUID organizationId, String email, String passwordHash, String displayName) {
    this.organizationId = organizationId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.status = UserStatus.PENDING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected User() {}

  /** Marks the user as active after email verification or invitation acceptance. */
  public void activate() {
    this.status = UserStatus.ACTIVE;
    this.emailVerifiedAt = emailVerifiedAt == null ? Instant.now() : emailVerifiedAt;
    this.updatedAt = Instant.now();
  }

  /** Disables the user account. */
  public void disable(UUID byUser) {
    this.status = UserStatus.DISABLED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Locks the user account after repeated failures. */
  public void lock() {
    this.status = UserStatus.LOCKED;
    this.updatedAt = Instant.now();
  }

  /** Unlocks the user account. */
  public void unlock(UUID byUser) {
    this.status = UserStatus.ACTIVE;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Replaces the password hash and records the change. */
  public void changePassword(String newPasswordHash, UUID byUser) {
    this.passwordHash = newPasswordHash;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Records a successful login, resetting the failure counter. */
  @SuppressWarnings("PMD.NullAssignment")
  public void markLoggedIn() {
    this.lastLoginAt = Instant.now();
    this.failedLoginAttempts = 0;
    this.lockedUntil = null;
    this.updatedAt = Instant.now();
  }

  /** Records a failed login attempt, locking the account once the threshold is exceeded. */
  public void recordFailedLogin(int maxAttempts, Duration lockDuration) {
    this.failedLoginAttempts += 1;
    if (this.failedLoginAttempts >= maxAttempts) {
      this.lockedUntil = Instant.now().plus(lockDuration);
    }
    this.updatedAt = Instant.now();
  }

  /** Returns true when the account is temporarily locked out. */
  public boolean isLockedOut() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
  }

  /** Updates the display name. */
  public void rename(String newDisplayName, UUID byUser) {
    this.displayName = newDisplayName;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the account as soft deleted. */
  public void delete(UUID byUser) {
    this.deletedBy = byUser;
    this.deletedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Returns true when the account is not soft deleted. */
  public boolean isActive() {
    return deletedAt == null;
  }

  /** Records when a password reset email was dispatched. */
  public void recordPasswordResetRequest() {
    this.passwordResetRequestedAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  /** Returns true when a reset email was dispatched within the given interval. */
  public boolean recentlyRequestedReset(Duration interval) {
    return passwordResetRequestedAt != null
        && passwordResetRequestedAt.isAfter(Instant.now().minus(interval));
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
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

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public Instant getPasswordResetRequestedAt() {
    return passwordResetRequestedAt;
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
