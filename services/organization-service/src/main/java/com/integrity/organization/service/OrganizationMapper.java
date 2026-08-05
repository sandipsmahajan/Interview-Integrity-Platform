package com.integrity.organization.service;

import com.integrity.organization.domain.Department;
import com.integrity.organization.domain.Organization;
import com.integrity.organization.domain.OrganizationAddress;
import com.integrity.organization.domain.OrganizationDomain;
import com.integrity.organization.domain.Plan;
import com.integrity.organization.domain.Subscription;
import com.integrity.organization.domain.Team;
import com.integrity.organization.domain.TeamMember;
import com.integrity.organization.web.dto.AddressResponse;
import com.integrity.organization.web.dto.DepartmentResponse;
import com.integrity.organization.web.dto.DomainResponse;
import com.integrity.organization.web.dto.OrganizationResponse;
import com.integrity.organization.web.dto.PlanResponse;
import com.integrity.organization.web.dto.SubscriptionResponse;
import com.integrity.organization.web.dto.TeamMemberResponse;
import com.integrity.organization.web.dto.TeamResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps organization-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface OrganizationMapper {

  /** Maps an organization into its public response. */
  OrganizationResponse toResponse(Organization organization);

  /** Maps an organization address into its public response. */
  AddressResponse toAddressResponse(OrganizationAddress address);

  /** Maps an organization domain into its public response. */
  DomainResponse toDomainResponse(OrganizationDomain domain);

  /** Maps a department into its public response. */
  DepartmentResponse toResponse(Department department);

  /** Maps a team into its public response. */
  TeamResponse toResponse(Team team);

  /** Maps a team member into its public response. */
  TeamMemberResponse toMemberResponse(TeamMember member);

  /** Maps a plan into its public response. */
  PlanResponse toResponse(Plan plan);

  /** Maps a subscription and its plan into the public response. */
  @Mapping(target = "id", source = "subscription.id")
  @Mapping(target = "organizationId", source = "subscription.organizationId")
  @Mapping(target = "planCode", source = "plan.code")
  @Mapping(target = "planName", source = "plan.name")
  SubscriptionResponse toResponse(Subscription subscription, Plan plan);
}
