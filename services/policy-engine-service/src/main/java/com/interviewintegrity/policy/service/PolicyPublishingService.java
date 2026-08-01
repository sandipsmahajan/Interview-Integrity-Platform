package com.interviewintegrity.policy.service;

import com.interviewintegrity.policy.domain.Policy;
import com.interviewintegrity.policy.domain.PolicyRule;
import com.interviewintegrity.policy.domain.PolicyVersion;
import com.interviewintegrity.policy.repository.PolicyRuleRepository;
import com.interviewintegrity.policy.repository.PolicyVersionRepository;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Snapshot service producing immutable policy versions on publication. */
public class PolicyPublishingService {

  private final PolicyRuleRepository ruleRepository;
  private final PolicyVersionRepository versionRepository;
  private final ObjectMapper objectMapper;

  /** Wires the service with its repositories. */
  public PolicyPublishingService(
      PolicyRuleRepository ruleRepository, PolicyVersionRepository versionRepository) {
    this.ruleRepository = ruleRepository;
    this.versionRepository = versionRepository;
    this.objectMapper = new ObjectMapper();
  }

  /** Snapshots a policy and its rules as the next version. */
  public Mono<PolicyVersion> publishVersion(Policy policy, UUID publishedBy) {
    return ruleRepository
        .listByPolicy(policy.getId())
        .collectList()
        .flatMap(
            rules ->
                versionRepository
                    .latestVersion(policy.getId())
                    .flatMap(
                        latest ->
                            versionRepository.insert(
                                new PolicyVersion(
                                    policy.getOrganizationId(),
                                    policy.getId(),
                                    latest + 1,
                                    definition(policy, rules),
                                    policy.getStatus(),
                                    publishedBy))));
  }

  private String definition(Policy policy, List<PolicyRule> rules) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("policyId", policy.getId().toString());
    root.put("code", policy.getCode());
    root.put("name", policy.getName());
    root.put("defaultSeverity", policy.getDefaultSeverity().name());
    ArrayNode ruleArray = root.putArray("rules");
    rules.forEach(
        rule -> {
          ObjectNode ruleNode = ruleArray.addObject();
          ruleNode.put("ruleCode", rule.getRuleCode());
          ruleNode.put("severity", rule.getSeverity().name());
          ruleNode.put("weight", rule.getWeight());
          ruleNode.put("orderIndex", rule.getOrderIndex());
          ruleNode.put("condition", rule.getCondition());
        });
    try {
      return objectMapper.writeValueAsString(root);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize policy definition", e);
    }
  }
}
