package com.interviewintegrity.candidate.config;

import com.interviewintegrity.candidate.repository.AssessmentRepository;
import com.interviewintegrity.candidate.repository.CandidateConsentRepository;
import com.interviewintegrity.candidate.repository.CandidateDocumentRepository;
import com.interviewintegrity.candidate.repository.CandidateNoteRepository;
import com.interviewintegrity.candidate.repository.CandidateProfileRepository;
import com.interviewintegrity.candidate.repository.CandidateRepository;
import com.interviewintegrity.candidate.repository.CandidateTagRepository;
import com.interviewintegrity.candidate.repository.TagRepository;
import com.interviewintegrity.candidate.service.AssessmentService;
import com.interviewintegrity.candidate.service.CandidateConsentService;
import com.interviewintegrity.candidate.service.CandidateDocumentService;
import com.interviewintegrity.candidate.service.CandidateEventPublisher;
import com.interviewintegrity.candidate.service.CandidateNoteService;
import com.interviewintegrity.candidate.service.CandidateProfileService;
import com.interviewintegrity.candidate.service.CandidateService;
import com.interviewintegrity.candidate.service.KafkaCandidateEventPublisher;
import com.interviewintegrity.candidate.service.TagService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.kafka.sender.KafkaSender;

/**
 * Explicit bean wiring for the candidate service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the database client backed candidate-tag bridge repository. */
  @Bean
  public CandidateTagRepository candidateTagRepository(DatabaseClient databaseClient) {
    return new CandidateTagRepository(databaseClient);
  }

  /** Provides the event publisher for candidate lifecycle events. */
  @Bean
  public CandidateEventPublisher candidateEventPublisher(
      KafkaSender<String, String> sender, Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "candidate-service");
    return new KafkaCandidateEventPublisher(sender, serviceName);
  }

  /** Provides the candidate service. */
  @Bean
  public CandidateService candidateService(
      CandidateRepository candidateRepository, CandidateEventPublisher eventPublisher) {
    return new CandidateService(candidateRepository, eventPublisher);
  }

  /** Provides the candidate profile service. */
  @Bean
  public CandidateProfileService candidateProfileService(
      CandidateProfileRepository profileRepository, CandidateService candidateService) {
    return new CandidateProfileService(profileRepository, candidateService);
  }

  /** Provides the candidate document service. */
  @Bean
  public CandidateDocumentService candidateDocumentService(
      CandidateDocumentRepository documentRepository, CandidateService candidateService) {
    return new CandidateDocumentService(documentRepository, candidateService);
  }

  /** Provides the candidate note service. */
  @Bean
  public CandidateNoteService candidateNoteService(
      CandidateNoteRepository noteRepository, CandidateService candidateService) {
    return new CandidateNoteService(noteRepository, candidateService);
  }

  /** Provides the assessment service. */
  @Bean
  public AssessmentService assessmentService(
      AssessmentRepository assessmentRepository, CandidateService candidateService) {
    return new AssessmentService(assessmentRepository, candidateService);
  }

  /** Provides the candidate consent service. */
  @Bean
  public CandidateConsentService candidateConsentService(
      CandidateConsentRepository consentRepository, CandidateService candidateService) {
    return new CandidateConsentService(consentRepository, candidateService);
  }

  /** Provides the tag service. */
  @Bean
  public TagService tagService(
      TagRepository tagRepository,
      CandidateTagRepository candidateTagRepository,
      CandidateService candidateService) {
    return new TagService(tagRepository, candidateTagRepository, candidateService);
  }
}
