package com.integrity.interview.config;

import com.integrity.interview.service.InterviewCalendarEventService;
import com.integrity.interview.service.InterviewFeedbackService;
import com.integrity.interview.service.InterviewMapper;
import com.integrity.interview.service.InterviewPanelService;
import com.integrity.interview.service.InterviewService;
import com.integrity.interview.service.InterviewSessionService;
import com.integrity.interview.service.InterviewerService;
import com.integrity.interview.web.InterviewCalendarEventController;
import com.integrity.interview.web.InterviewController;
import com.integrity.interview.web.InterviewFeedbackController;
import com.integrity.interview.web.InterviewPanelController;
import com.integrity.interview.web.InterviewSessionController;
import com.integrity.interview.web.InterviewerController;
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

  /** Exposes the interview controller. */
  @Bean
  public InterviewController interviewController(
      InterviewService interviewService, InterviewMapper mapper) {
    return new InterviewController(interviewService, mapper);
  }

  /** Exposes the interview session controller. */
  @Bean
  public InterviewSessionController interviewSessionController(
      InterviewSessionService sessionService, InterviewMapper mapper) {
    return new InterviewSessionController(sessionService, mapper);
  }

  /** Exposes the interviewer controller. */
  @Bean
  public InterviewerController interviewerController(
      InterviewerService interviewerService, InterviewMapper mapper) {
    return new InterviewerController(interviewerService, mapper);
  }

  /** Exposes the panel controller. */
  @Bean
  public InterviewPanelController interviewPanelController(InterviewPanelService panelService) {
    return new InterviewPanelController(panelService);
  }

  /** Exposes the feedback controller. */
  @Bean
  public InterviewFeedbackController interviewFeedbackController(
      InterviewFeedbackService feedbackService, InterviewMapper mapper) {
    return new InterviewFeedbackController(feedbackService, mapper);
  }

  /** Exposes the calendar event controller. */
  @Bean
  public InterviewCalendarEventController interviewCalendarEventController(
      InterviewCalendarEventService calendarService, InterviewMapper mapper) {
    return new InterviewCalendarEventController(calendarService, mapper);
  }

  /** Describes the OpenAPI document for the interview service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Interview Service API")
                .version("v1")
                .description("Interview records, monitoring sessions, panels and feedback"))
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
