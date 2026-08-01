package com.interviewintegrity.recruiter.config;

import com.interviewintegrity.recruiter.repository.CandidatePipelineRepository;
import com.interviewintegrity.recruiter.repository.PipelineStageRepository;
import com.interviewintegrity.recruiter.repository.RecruiterAssignmentRepository;
import com.interviewintegrity.recruiter.repository.RecruiterNoteRepository;
import com.interviewintegrity.recruiter.repository.RecruiterProfileRepository;
import com.interviewintegrity.recruiter.repository.RecruiterRepository;
import com.interviewintegrity.recruiter.service.PipelineService;
import com.interviewintegrity.recruiter.service.RecruiterAssignmentService;
import com.interviewintegrity.recruiter.service.RecruiterNoteService;
import com.interviewintegrity.recruiter.service.RecruiterProfileService;
import com.interviewintegrity.recruiter.service.RecruiterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the recruiter service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the recruiter service. */
  @Bean
  public RecruiterService recruiterService(RecruiterRepository recruiterRepository) {
    return new RecruiterService(recruiterRepository);
  }

  /** Provides the recruiter profile service. */
  @Bean
  public RecruiterProfileService recruiterProfileService(
      RecruiterProfileRepository profileRepository) {
    return new RecruiterProfileService(profileRepository);
  }

  /** Provides the pipeline service. */
  @Bean
  public PipelineService pipelineService(
      PipelineStageRepository stageRepository,
      CandidatePipelineRepository pipelineRepository,
      RecruiterRepository recruiterRepository) {
    return new PipelineService(stageRepository, pipelineRepository, recruiterRepository);
  }

  /** Provides the note service. */
  @Bean
  public RecruiterNoteService recruiterNoteService(
      RecruiterNoteRepository noteRepository, RecruiterRepository recruiterRepository) {
    return new RecruiterNoteService(noteRepository, recruiterRepository);
  }

  /** Provides the assignment service. */
  @Bean
  public RecruiterAssignmentService recruiterAssignmentService(
      RecruiterAssignmentRepository assignmentRepository) {
    return new RecruiterAssignmentService(assignmentRepository);
  }
}
