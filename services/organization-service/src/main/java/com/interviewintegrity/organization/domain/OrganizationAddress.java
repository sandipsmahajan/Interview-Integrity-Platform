package com.interviewintegrity.organization.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** One-to-one registered billing address of an organization. */
@Table("organization_addresses")
public class OrganizationAddress implements Persistable<UUID> {

  @Id private UUID id;

  @Column("organization_id")
  private UUID organizationId;

  private String line1;
  private String line2;
  private String city;
  private String region;

  @Column("postal_code")
  private String postalCode;

  @Column("country_code")
  private String countryCode;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Version private long version = 1;

  /** Creates an address for the given organization. */
  public OrganizationAddress(UUID organizationId) {
    this.organizationId = organizationId;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  protected OrganizationAddress() {}

  /** Replaces the address details. */
  public void update(
      String line1,
      String line2,
      String city,
      String region,
      String postalCode,
      String countryCode) {
    this.line1 = line1;
    this.line2 = line2;
    this.city = city;
    this.region = region;
    this.postalCode = postalCode;
    String normalizedCountryCode = countryCode;
    if (countryCode != null) {
      normalizedCountryCode = countryCode.toUpperCase(Locale.ROOT);
    }
    this.countryCode = normalizedCountryCode;
    this.updatedAt = Instant.now();
  }

  @Override
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public String getCity() {
    return city;
  }

  public String getRegion() {
    return region;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountryCode() {
    return countryCode;
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
