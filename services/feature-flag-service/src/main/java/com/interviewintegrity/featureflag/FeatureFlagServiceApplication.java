package com.interviewintegrity.featureflag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the feature flag service (flags, rollouts, experiments). */
@SpringBootApplication
public class FeatureFlagServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FeatureFlagServiceApplication.class, args);
  }
}
