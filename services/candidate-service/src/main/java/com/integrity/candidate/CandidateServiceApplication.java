package com.integrity.candidate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the candidate service (profiles, documents, applications). */
@SpringBootApplication
public class CandidateServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CandidateServiceApplication.class, args);
  }
}
