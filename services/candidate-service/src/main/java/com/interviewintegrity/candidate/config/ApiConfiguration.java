package com.interviewintegrity.candidate.config;

import com.interviewintegrity.candidate.service.AssessmentService;
import com.interviewintegrity.candidate.service.CandidateConsentService;
import com.interviewintegrity.candidate.service.CandidateDocumentService;
import com.interviewintegrity.candidate.service.CandidateNoteService;
import com.interviewintegrity.candidate.service.CandidateProfileService;
import com.interviewintegrity.candidate.service.CandidateService;
import com.interviewintegrity.candidate.service.TagService;
import com.interviewintegrity.candidate.web.AssessmentController;
import com.interviewintegrity.candidate.web.CandidateConsentController;
import com.interviewintegrity.candidate.web.CandidateController;
import com.interviewintegrity.candidate.web.CandidateDocumentController;
import com.interviewintegrity.candidate.web.CandidateNoteController;
import com.interviewintegrity.candidate.web.CandidateProfileController;
import com.interviewintegrity.candidate.web.CandidateTagController;
import com.interviewintegrity.candidate.web.TagController;
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

  /** Exposes the candidate controller. */
  @Bean
  public CandidateController candidateController(CandidateService candidateService) {
    return new CandidateController(candidateService);
  }

  /** Exposes the candidate profile controller. */
  @Bean
  public CandidateProfileController candidateProfileController(
      CandidateProfileService profileService) {
    return new CandidateProfileController(profileService);
  }

  /** Exposes the candidate document controller. */
  @Bean
  public CandidateDocumentController candidateDocumentController(
      CandidateDocumentService documentService) {
    return new CandidateDocumentController(documentService);
  }

  /** Exposes the candidate note controller. */
  @Bean
  public CandidateNoteController candidateNoteController(CandidateNoteService noteService) {
    return new CandidateNoteController(noteService);
  }

  /** Exposes the assessment controller. */
  @Bean
  public AssessmentController assessmentController(AssessmentService assessmentService) {
    return new AssessmentController(assessmentService);
  }

  /** Exposes the candidate consent controller. */
  @Bean
  public CandidateConsentController candidateConsentController(
      CandidateConsentService consentService) {
    return new CandidateConsentController(consentService);
  }

  /** Exposes the tag controller. */
  @Bean
  public TagController tagController(TagService tagService) {
    return new TagController(tagService);
  }

  /** Exposes the candidate tag controller. */
  @Bean
  public CandidateTagController candidateTagController(TagService tagService) {
    return new CandidateTagController(tagService);
  }

  /** Describes the OpenAPI document for the candidate service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Candidate Service API")
                .version("v1")
                .description("Candidate profiles, documents, assessments, consents and tags"))
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
