package com.integrity.featureflag.domain;

import com.integrity.validation.Assert;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** A/B experiment driving evidence based flag rollouts. */
@Table("experiments")
public class Experiment implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String name;

  @Column("feature_id")
  private UUID featureId;

  @Column("control_variant")
  private String controlVariant;

  @Column("treatment_variant")
  private String treatmentVariant;

  private ExperimentStatus status;

  @Column("started_at")
  private Instant startedAt;

  @Column("ended_at")
  private Instant endedAt;

  private Json metrics;

  @Column("created_by")
  private UUID createdBy;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_by")
  private UUID updatedBy;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates a draft experiment. */
  public Experiment(
      UUID organizationId,
      String name,
      UUID featureId,
      String controlVariant,
      String treatmentVariant,
      String metrics,
      UUID createdBy) {
    Assert.notBlank(name, "name");
    Assert.notNull(featureId, "featureId");
    this.organizationId = organizationId;
    this.name = name;
    this.featureId = featureId;
    this.controlVariant = controlVariant;
    this.treatmentVariant = treatmentVariant;
    this.status = ExperimentStatus.DRAFT;
    this.metrics = Json.of(metrics == null ? "{}" : metrics);
    this.createdBy = createdBy;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected Experiment() {}

  /** Starts the experiment, either from draft or a paused state. */
  public void start(UUID byUser) {
    Assert.isTrue(
        status == ExperimentStatus.DRAFT || status == ExperimentStatus.PAUSED,
        "Only DRAFT or PAUSED experiments can be started");
    this.status = ExperimentStatus.RUNNING;
    this.startedAt = Instant.now();
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Pauses a running experiment. */
  public void pause(UUID byUser) {
    Assert.isTrue(status == ExperimentStatus.RUNNING, "Only RUNNING experiments can be paused");
    this.status = ExperimentStatus.PAUSED;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Resumes a paused experiment. */
  public void resume(UUID byUser) {
    Assert.isTrue(status == ExperimentStatus.PAUSED, "Only PAUSED experiments can be resumed");
    this.status = ExperimentStatus.RUNNING;
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Completes the experiment. */
  public void complete(UUID byUser) {
    Assert.isTrue(
        status == ExperimentStatus.RUNNING || status == ExperimentStatus.PAUSED,
        "Only RUNNING or PAUSED experiments can be completed");
    this.status = ExperimentStatus.COMPLETED;
    this.endedAt = Instant.now();
    this.updatedBy = byUser;
    this.updatedAt = Instant.now();
  }

  /** Rejects the experiment. */
  public void reject(UUID byUser) {
    this.status = ExperimentStatus.REJECTED;
    this.endedAt = Instant.now();
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

  public String getName() {
    return name;
  }

  public UUID getFeatureId() {
    return featureId;
  }

  public String getControlVariant() {
    return controlVariant;
  }

  public String getTreatmentVariant() {
    return treatmentVariant;
  }

  public ExperimentStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public String getMetrics() {
    return metrics == null ? "{}" : metrics.asString();
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
