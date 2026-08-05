package com.integrity.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the integration service (providers, credentials, webhooks). */
@SpringBootApplication
public class IntegrationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IntegrationServiceApplication.class, args);
  }
}
