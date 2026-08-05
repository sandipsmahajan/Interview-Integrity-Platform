package com.integrity.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the report service (report generation, scheduling). */
@SpringBootApplication
public class ReportServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReportServiceApplication.class, args);
  }
}
