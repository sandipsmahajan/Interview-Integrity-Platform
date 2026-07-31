package com.interviewintegrity.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Registers the common infrastructure beans shared by every service. */
@AutoConfiguration
public class CommonAutoConfiguration {

  /** Provides the system-clock backed {@link TimeProvider}. */
  @Bean
  public TimeProvider timeProvider() {
    return new SystemTimeProvider();
  }
}
