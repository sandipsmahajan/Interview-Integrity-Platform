package com.interviewintegrity.interview.config;

import com.interviewintegrity.interview.service.InterviewCalendarEventService;
import com.interviewintegrity.interview.service.InterviewFeedbackService;
import com.interviewintegrity.interview.service.InterviewPanelService;
import com.interviewintegrity.interview.service.InterviewService;
import com.interviewintegrity.interview.service.InterviewSessionService;
import com.interviewintegrity.interview.service.InterviewerService;
import com.interviewintegrity.interview.web.InterviewCalendarEventController;
import com.interviewintegrity.interview.web.InterviewController;
import com.interviewintegrity.interview.web.InterviewFeedbackController;
import com.interviewintegrity.interview.web.InterviewPanelController;
import com.interviewintegrity.interview.web.InterviewSessionController;
import com.interviewintegrity.interview.web.InterviewerController;
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
  public InterviewController interviewController(InterviewService interviewService) {
    return new InterviewController(interviewService);
  }

  /** Exposes the interview session controller. */
  @Bean
  public InterviewSessionController interviewSessionController(
      InterviewSessionService sessionService) {
    return new InterviewSessionController(sessionService);
  }

  /** Exposes the interviewer controller. */
  @Bean
  public InterviewerController interviewerController(InterviewerService interviewerService) {
    return new InterviewerController(interviewerService);
  }

  /** Exposes the panel controller. */
  @Bean
  public InterviewPanelController interviewPanelController(InterviewPanelService panelService) {
    return new InterviewPanelController(panelService);
  }

  /** Exposes the feedback controller. */
  @Bean
  public InterviewFeedbackController interviewFeedbackController(
      InterviewFeedbackService feedbackService) {
    return new InterviewFeedbackController(feedbackService);
  }

  /** Exposes the calendar event controller. */
  @Bean
  public InterviewCalendarEventController interviewCalendarEventController(
      InterviewCalendarEventService calendarService) {
    return new InterviewCalendarEventController(calendarService);
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
