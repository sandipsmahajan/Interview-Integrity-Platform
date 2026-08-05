package com.integrity.interview.service;

import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewCalendarEvent;
import com.integrity.interview.domain.InterviewFeedback;
import com.integrity.interview.domain.InterviewPanel;
import com.integrity.interview.domain.InterviewSession;
import com.integrity.interview.domain.Interviewer;
import com.integrity.interview.web.dto.InterviewCalendarEventResponse;
import com.integrity.interview.web.dto.InterviewFeedbackResponse;
import com.integrity.interview.web.dto.InterviewPanelResponse;
import com.integrity.interview.web.dto.InterviewResponse;
import com.integrity.interview.web.dto.InterviewSessionResponse;
import com.integrity.interview.web.dto.InterviewerResponse;
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
