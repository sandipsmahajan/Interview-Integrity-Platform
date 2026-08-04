package com.interviewintegrity.organization.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** The single active subscription of an organization. */
@Table("subscriptions")
public class Subscription implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  @Column("plan_id")
  private UUID planId;

  private SubscriptionStatus status;

  @Column("current_period_start")
  private LocalDate currentPeriodStart;

  @Column("current_period_end")
  private LocalDate currentPeriodEnd;

  @Column("cancel_at_period_end")
  private boolean cancelAtPeriodEnd;

  private String provider;

  @Column("provider_subscription_id")
  private String providerSubscriptionId;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a trial subscription for the organization. */
  public Subscription(
      UUID organizationId,
      UUID planId,
      UUID createdBy,
      LocalDate currentPeriodStart,
      LocalDate currentPeriodEnd) {
    this.organizationId = organizationId;
    this.planId = planId;
    this.createdBy = createdBy;
    this.currentPeriodStart = currentPeriodStart;
    this.currentPeriodEnd = currentPeriodEnd;
    this.status = SubscriptionStatus.TRIALING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Subscription() {}

  /** Schedules cancellation at the end of the current period. */
  public void scheduleCancellation(UUID byUser) {
    this.cancelAtPeriodEnd = true;
    this.status = SubscriptionStatus.CANCELED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Reverts a scheduled cancellation. */
  public void resume(UUID byUser) {
    this.cancelAtPeriodEnd = false;
    if (this.status == SubscriptionStatus.CANCELED) {
      this.status = SubscriptionStatus.ACTIVE;
    }
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the subscription as actively paid. */
  public void markActive(UUID byUser) {
    this.status = SubscriptionStatus.ACTIVE;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Marks the subscription as past due after a failed payment. */
  public void markPastDue(UUID byUser) {
    this.status = SubscriptionStatus.PAST_DUE;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Rolls the subscription into a new billing period. */
  public void renew(LocalDate newPeriodStart, LocalDate newPeriodEnd) {
    this.currentPeriodStart = newPeriodStart;
    this.currentPeriodEnd = newPeriodEnd;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getPlanId() {
    return planId;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public LocalDate getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public LocalDate getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public boolean isCancelAtPeriodEnd() {
    return cancelAtPeriodEnd;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderSubscriptionId() {
    return providerSubscriptionId;
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
