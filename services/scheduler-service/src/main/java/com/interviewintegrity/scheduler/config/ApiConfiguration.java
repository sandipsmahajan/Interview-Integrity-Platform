package com.interviewintegrity.scheduler.config;

import com.interviewintegrity.scheduler.service.JobExecutionService;
import com.interviewintegrity.scheduler.service.JobLockService;
import com.interviewintegrity.scheduler.service.ScheduledJobService;
import com.interviewintegrity.scheduler.service.SchedulerMapper;
import com.interviewintegrity.scheduler.web.JobExecutionController;
import com.interviewintegrity.scheduler.web.JobLockController;
import com.interviewintegrity.scheduler.web.ScheduledJobController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the scheduled job controller. */
  @Bean
  public ScheduledJobController scheduledJobController(
      ScheduledJobService jobService, SchedulerMapper mapper) {
    return new ScheduledJobController(jobService, mapper);
  }

  /** Exposes the job execution controller. */
  @Bean
  public JobExecutionController jobExecutionController(
      JobExecutionService executionService, SchedulerMapper mapper) {
    return new JobExecutionController(executionService, mapper);
  }

  /** Exposes the job lock controller. */
  @Bean
  public JobLockController jobLockController(JobLockService lockService) {
    return new JobLockController(lockService);
  }

  /** Describes the OpenAPI document for the scheduler service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Scheduler Service API")
                .version("v1")
                .description("Scheduled job definitions, execution tracking and distributed locks"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
