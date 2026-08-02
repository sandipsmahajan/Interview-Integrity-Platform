package com.interviewintegrity.policy.service;

import com.interviewintegrity.policy.domain.Policy;
import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.Violation;
import com.interviewintegrity.policy.web.dto.PolicyResponse;
import com.interviewintegrity.policy.web.dto.RuleResponse;
import com.interviewintegrity.policy.web.dto.ViolationResponse;
import org.mapstruct.Mapper;

/**
 * Maps policy-engine domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface PolicyMapper {

  /** Maps a policy into its public response. */
  PolicyResponse toResponse(Policy policy);

  /** Maps a policy rule into its public response. */
  RuleResponse toResponse(PolicyRule rule);

  /** Maps a violation into its public response. */
  ViolationResponse toResponse(Violation violation);
}
