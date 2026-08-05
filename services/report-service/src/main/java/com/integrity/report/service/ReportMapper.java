package com.integrity.report.service;

import com.integrity.report.domain.Report;
import com.integrity.report.domain.ReportRequest;
import com.integrity.report.domain.ReportSchedule;
import com.integrity.report.domain.ReportSection;
import com.integrity.report.web.dto.ReportRequestResponse;
import com.integrity.report.web.dto.ReportResponse;
import com.integrity.report.web.dto.ReportScheduleResponse;
import com.integrity.report.web.dto.ReportSectionResponse;
import org.mapstruct.Mapper;

/**
 * Maps report-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface ReportMapper {

  /** Maps a report into its public response. */
  ReportResponse toResponse(Report report);

  /** Maps a report section into its public response. */
  ReportSectionResponse toResponse(ReportSection section);

  /** Maps a report request into its public response. */
  ReportRequestResponse toResponse(ReportRequest request);

  /** Maps a report schedule into its public response. */
  ReportScheduleResponse toResponse(ReportSchedule schedule);
}
