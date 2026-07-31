package com.interviewintegrity.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the scheduler service (jobs, executions, distributed locks). */
@SpringBootApplication
public class SchedulerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchedulerServiceApplication.class, args);
  }
}
