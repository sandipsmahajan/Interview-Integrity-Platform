package com.integrity.candidate.service;

import com.integrity.candidate.domain.Assessment;
import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.CandidateConsent;
import com.integrity.candidate.domain.CandidateDocument;
import com.integrity.candidate.domain.CandidateNote;
import com.integrity.candidate.domain.CandidateProfile;
import com.integrity.candidate.domain.Tag;
import com.integrity.candidate.web.dto.AssessmentResponse;
import com.integrity.candidate.web.dto.CandidateConsentResponse;
import com.integrity.candidate.web.dto.CandidateDocumentResponse;
import com.integrity.candidate.web.dto.CandidateNoteResponse;
import com.integrity.candidate.web.dto.CandidateProfileResponse;
import com.integrity.candidate.web.dto.CandidateResponse;
import com.integrity.candidate.web.dto.TagResponse;
import org.mapstruct.Mapper;

/**
 * Maps candidate-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time; the skills array is converted into a list automatically.
 */
@Mapper(componentModel = "spring")
public interface CandidateMapper {

  /** Maps a candidate into its public response. */
  CandidateResponse toResponse(Candidate candidate);

  /** Maps a candidate profile into its public response. */
  CandidateProfileResponse toResponse(CandidateProfile profile);

  /** Maps a candidate document into its public response. */
  CandidateDocumentResponse toResponse(CandidateDocument document);

  /** Maps a candidate note into its public response. */
  CandidateNoteResponse toResponse(CandidateNote note);

  /** Maps an assessment into its public response. */
  AssessmentResponse toResponse(Assessment assessment);

  /** Maps a candidate consent into its public response. */
  CandidateConsentResponse toResponse(CandidateConsent consent);

  /** Maps a tag into its public response. */
  TagResponse toResponse(Tag tag);
}
