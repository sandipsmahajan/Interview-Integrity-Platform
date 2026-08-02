package com.interviewintegrity.scheduler.service;

import com.interviewintegrity.scheduler.domain.JobExecution;
import com.interviewintegrity.scheduler.domain.ScheduledJob;
import com.interviewintegrity.scheduler.web.dto.JobExecutionResponse;
import com.interviewintegrity.scheduler.web.dto.ScheduledJobResponse;
import org.mapstruct.Mapper;

/**
 * Maps scheduler-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface SchedulerMapper {

  /** Maps a scheduled job into its public response. */
  ScheduledJobResponse toResponse(ScheduledJob job);

  /** Maps a job execution into its public response. */
  JobExecutionResponse toResponse(JobExecution execution);
}
