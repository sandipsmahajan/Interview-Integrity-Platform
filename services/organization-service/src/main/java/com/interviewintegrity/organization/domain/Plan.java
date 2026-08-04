package com.interviewintegrity.organization.domain;

import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Subscription plan offered by the platform (global catalog, read only). */
@Table("plans")
public class Plan implements Persistable<UUID> {

  @Id private UUID id;

  private String code;
  private String name;

  @Column("monthly_price_cents")
  private long monthlyPriceCents;

  @Column("max_seats")
  private Integer maxSeats;

  private Json features;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  @Override
  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public long getMonthlyPriceCents() {
    return monthlyPriceCents;
  }

  public Integer getMaxSeats() {
    return maxSeats;
  }

  public String getFeatures() {
    return features == null ? "{}" : features.asString();
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
