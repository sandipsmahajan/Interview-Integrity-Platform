package com.integrity.policy.service;

import com.integrity.policy.domain.Policy;
import com.integrity.policy.domain.PolicyRule;
import com.integrity.policy.domain.Violation;
import com.integrity.policy.web.dto.PolicyResponse;
import com.integrity.policy.web.dto.RuleResponse;
import com.integrity.policy.web.dto.ViolationResponse;
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
