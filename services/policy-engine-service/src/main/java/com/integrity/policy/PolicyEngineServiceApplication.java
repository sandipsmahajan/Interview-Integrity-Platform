package com.integrity.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the policy engine (rules, violations, escalations). */
@SpringBootApplication
public class PolicyEngineServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PolicyEngineServiceApplication.class, args);
  }
}
