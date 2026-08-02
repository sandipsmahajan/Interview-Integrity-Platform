package com.interviewintegrity.interview.service;

import com.interviewintegrity.interview.domain.Interview;
import com.interviewintegrity.interview.domain.InterviewCalendarEvent;
import com.interviewintegrity.interview.domain.InterviewFeedback;
import com.interviewintegrity.interview.domain.InterviewPanel;
import com.interviewintegrity.interview.domain.InterviewSession;
import com.interviewintegrity.interview.domain.Interviewer;
import com.interviewintegrity.interview.web.dto.InterviewCalendarEventResponse;
import com.interviewintegrity.interview.web.dto.InterviewFeedbackResponse;
import com.interviewintegrity.interview.web.dto.InterviewPanelResponse;
import com.interviewintegrity.interview.web.dto.InterviewResponse;
import com.interviewintegrity.interview.web.dto.InterviewSessionResponse;
import com.interviewintegrity.interview.web.dto.InterviewerResponse;
import org.mapstruct.Mapper;

/**
 * Maps interview-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface InterviewMapper {

  /** Maps an interview into its public response. */
  InterviewResponse toResponse(Interview interview);

  /** Maps an interview session into its public response. */
  InterviewSessionResponse toResponse(InterviewSession session);

  /** Maps an interviewer into its public response. */
  InterviewerResponse toResponse(Interviewer interviewer);

  /** Maps an interview feedback into its public response. */
  InterviewFeedbackResponse toResponse(InterviewFeedback feedback);

  /** Maps an interview calendar event into its public response. */
  InterviewCalendarEventResponse toResponse(InterviewCalendarEvent event);

  /** Maps an interview panel entry into its public response. */
  InterviewPanelResponse toResponse(InterviewPanel panel);
}
