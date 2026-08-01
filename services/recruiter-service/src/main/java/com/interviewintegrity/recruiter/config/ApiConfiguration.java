package com.interviewintegrity.recruiter.config;

import com.interviewintegrity.recruiter.service.PipelineService;
import com.interviewintegrity.recruiter.service.RecruiterAssignmentService;
import com.interviewintegrity.recruiter.service.RecruiterNoteService;
import com.interviewintegrity.recruiter.service.RecruiterProfileService;
import com.interviewintegrity.recruiter.service.RecruiterService;
import com.interviewintegrity.recruiter.web.CandidatePipelineController;
import com.interviewintegrity.recruiter.web.PipelineStageController;
import com.interviewintegrity.recruiter.web.RecruiterAssignmentController;
import com.interviewintegrity.recruiter.web.RecruiterController;
import com.interviewintegrity.recruiter.web.RecruiterNoteController;
import com.interviewintegrity.recruiter.web.RecruiterProfileController;
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

  /** Exposes the recruiter controller. */
  @Bean
  public RecruiterController recruiterController(RecruiterService recruiterService) {
    return new RecruiterController(recruiterService);
  }

  /** Exposes the recruiter profile controller. */
  @Bean
  public RecruiterProfileController recruiterProfileController(
      RecruiterProfileService profileService) {
    return new RecruiterProfileController(profileService);
  }

  /** Exposes the pipeline stage controller. */
  @Bean
  public PipelineStageController pipelineStageController(PipelineService pipelineService) {
    return new PipelineStageController(pipelineService);
  }

  /** Exposes the candidate pipeline controller. */
  @Bean
  public CandidatePipelineController candidatePipelineController(PipelineService pipelineService) {
    return new CandidatePipelineController(pipelineService);
  }

  /** Exposes the note controller. */
  @Bean
  public RecruiterNoteController recruiterNoteController(RecruiterNoteService noteService) {
    return new RecruiterNoteController(noteService);
  }

  /** Exposes the assignment controller. */
  @Bean
  public RecruiterAssignmentController recruiterAssignmentController(
      RecruiterAssignmentService assignmentService) {
    return new RecruiterAssignmentController(assignmentService);
  }

  /** Describes the OpenAPI document for the recruiter service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Recruiter Service API")
                .version("v1")
                .description("Recruiter profiles, pipeline stages and candidate assignments"))
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
