package com.integrity.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the telemetry service (event ingestion, rollups, retention). */
@SpringBootApplication
public class TelemetryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TelemetryServiceApplication.class, args);
  }
}
