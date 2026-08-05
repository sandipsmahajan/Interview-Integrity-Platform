package com.integrity.featureflag.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Row mapped explicit per-user override for a flag.
 *
 * <p>The bridge table uses a composite primary key, so it is read through {@code DatabaseClient}
 * SQL.
 */
@Table("flag_targets")
public class FlagTarget {

  @Column("flag_id")
  private UUID flagId;

  @Column("user_id")
  private UUID userId;

  private String variant;
  private boolean enabled;

  @Column("added_by")
  private UUID addedBy;

  @Column("added_at")
  private Instant addedAt;

  /** Creates a per-user flag override. */
  public FlagTarget(UUID flagId, UUID userId, String variant, boolean enabled, UUID addedBy) {
    this.flagId = flagId;
    this.userId = userId;
    this.variant = variant;
    this.enabled = enabled;
    this.addedBy = addedBy;
    this.addedAt = Instant.now();
  }

  protected FlagTarget() {}

  public UUID getFlagId() {
    return flagId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getVariant() {
    return variant;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public UUID getAddedBy() {
    return addedBy;
  }

  public Instant getAddedAt() {
    return addedAt;
  }
}
