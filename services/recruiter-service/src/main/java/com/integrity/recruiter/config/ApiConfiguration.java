package com.integrity.recruiter.config;

import com.integrity.recruiter.service.PipelineService;
import com.integrity.recruiter.service.RecruiterAssignmentService;
import com.integrity.recruiter.service.RecruiterMapper;
import com.integrity.recruiter.service.RecruiterNoteService;
import com.integrity.recruiter.service.RecruiterProfileService;
import com.integrity.recruiter.service.RecruiterService;
import com.integrity.recruiter.web.CandidatePipelineController;
import com.integrity.recruiter.web.PipelineStageController;
import com.integrity.recruiter.web.RecruiterAssignmentController;
import com.integrity.recruiter.web.RecruiterController;
import com.integrity.recruiter.web.RecruiterNoteController;
import com.integrity.recruiter.web.RecruiterProfileController;
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
  public RecruiterController recruiterController(
      RecruiterService recruiterService, RecruiterMapper mapper) {
    return new RecruiterController(recruiterService, mapper);
  }

  /** Exposes the recruiter profile controller. */
  @Bean
  public RecruiterProfileController recruiterProfileController(
      RecruiterProfileService profileService, RecruiterMapper mapper) {
    return new RecruiterProfileController(profileService, mapper);
  }

  /** Exposes the pipeline stage controller. */
  @Bean
  public PipelineStageController pipelineStageController(
      PipelineService pipelineService, RecruiterMapper mapper) {
    return new PipelineStageController(pipelineService, mapper);
  }

  /** Exposes the candidate pipeline controller. */
  @Bean
  public CandidatePipelineController candidatePipelineController(
      PipelineService pipelineService, RecruiterMapper mapper) {
    return new CandidatePipelineController(pipelineService, mapper);
  }

  /** Exposes the note controller. */
  @Bean
  public RecruiterNoteController recruiterNoteController(
      RecruiterNoteService noteService, RecruiterMapper mapper) {
    return new RecruiterNoteController(noteService, mapper);
  }

  /** Exposes the assignment controller. */
  @Bean
  public RecruiterAssignmentController recruiterAssignmentController(
      RecruiterAssignmentService assignmentService, RecruiterMapper mapper) {
    return new RecruiterAssignmentController(assignmentService, mapper);
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
