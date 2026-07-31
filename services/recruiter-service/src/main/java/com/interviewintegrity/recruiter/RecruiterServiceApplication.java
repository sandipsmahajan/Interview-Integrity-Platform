package com.interviewintegrity.recruiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the recruiter service (pipelines, requisitions). */
@SpringBootApplication
public class RecruiterServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RecruiterServiceApplication.class, args);
  }
}
