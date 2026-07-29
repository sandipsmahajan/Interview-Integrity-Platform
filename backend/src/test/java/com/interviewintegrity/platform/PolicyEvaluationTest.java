package com.interviewintegrity.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewintegrity.platform.api.ApiDtos.TelemetryIngestRequest;
import com.interviewintegrity.platform.application.PlatformServices.DefaultPolicyEvaluationService;
import com.interviewintegrity.platform.domain.DomainModel.TelemetryType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PolicyEvaluationTest {
  private final DefaultPolicyEvaluationService service = new DefaultPolicyEvaluationService();

  @Test
  void flagsStaleHeartbeat() {
    var request = new TelemetryIngestRequest(UUID.randomUUID(), TelemetryType.HEARTBEAT,
        Instant.now(), Map.of("secondsSincePreviousHeartbeat", 15));

    StepVerifier.create(service.evaluate(request))
        .assertNext(violation -> assertThat(violation.ruleCode).isEqualTo("HEARTBEAT_STALE"))
        .verifyComplete();
  }
}
