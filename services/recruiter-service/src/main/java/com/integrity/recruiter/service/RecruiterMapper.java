package com.integrity.recruiter.service;

import com.integrity.recruiter.domain.CandidatePipeline;
import com.integrity.recruiter.domain.PipelineStage;
import com.integrity.recruiter.domain.Recruiter;
import com.integrity.recruiter.domain.RecruiterAssignment;
import com.integrity.recruiter.domain.RecruiterNote;
import com.integrity.recruiter.domain.RecruiterProfile;
import com.integrity.recruiter.web.dto.CandidatePipelineResponse;
import com.integrity.recruiter.web.dto.PipelineStageResponse;
import com.integrity.recruiter.web.dto.RecruiterAssignmentResponse;
import com.integrity.recruiter.web.dto.RecruiterNoteResponse;
import com.integrity.recruiter.web.dto.RecruiterProfileResponse;
import com.integrity.recruiter.web.dto.RecruiterResponse;
import org.mapstruct.Mapper;

/**
 * Maps recruiter-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time; the specialties array is converted into a list automatically.
 */
@Mapper(componentModel = "spring")
public interface RecruiterMapper {

  /** Maps a recruiter entity into its public response. */
  RecruiterResponse toResponse(Recruiter recruiter);

  /** Maps a candidate pipeline entry into its public response. */
  CandidatePipelineResponse toResponse(CandidatePipeline entry);

  /** Maps a pipeline stage into its public response. */
  PipelineStageResponse toResponse(PipelineStage stage);

  /** Maps a recruiter assignment into its public response. */
  RecruiterAssignmentResponse toResponse(RecruiterAssignment assignment);

  /** Maps a recruiter note into its public response. */
  RecruiterNoteResponse toResponse(RecruiterNote note);

  /** Maps the extended recruiter profile into its public response. */
  RecruiterProfileResponse toResponse(RecruiterProfile profile);
}
