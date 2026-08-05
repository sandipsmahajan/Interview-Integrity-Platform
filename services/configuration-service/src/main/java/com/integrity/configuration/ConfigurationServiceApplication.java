package com.integrity.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the configuration service (schema catalog, app config). */
@SpringBootApplication
public class ConfigurationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigurationServiceApplication.class, args);
  }
}
