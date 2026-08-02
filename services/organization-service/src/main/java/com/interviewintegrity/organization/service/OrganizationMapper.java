package com.interviewintegrity.organization.service;

import com.interviewintegrity.organization.domain.Department;
import com.interviewintegrity.organization.domain.Organization;
import com.interviewintegrity.organization.domain.OrganizationAddress;
import com.interviewintegrity.organization.domain.OrganizationDomain;
import com.interviewintegrity.organization.domain.Plan;
import com.interviewintegrity.organization.domain.Subscription;
import com.interviewintegrity.organization.domain.Team;
import com.interviewintegrity.organization.domain.TeamMember;
import com.interviewintegrity.organization.web.dto.AddressResponse;
import com.interviewintegrity.organization.web.dto.DepartmentResponse;
import com.interviewintegrity.organization.web.dto.DomainResponse;
import com.interviewintegrity.organization.web.dto.OrganizationResponse;
import com.interviewintegrity.organization.web.dto.PlanResponse;
import com.interviewintegrity.organization.web.dto.SubscriptionResponse;
import com.interviewintegrity.organization.web.dto.TeamMemberResponse;
import com.interviewintegrity.organization.web.dto.TeamResponse;
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
