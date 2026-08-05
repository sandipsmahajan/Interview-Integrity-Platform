package com.integrity.report.service;

import com.integrity.exception.NotFoundException;
import com.integrity.report.domain.ReportFormat;
import com.integrity.report.domain.ReportSchedule;
import com.integrity.report.domain.ReportType;
import com.integrity.report.repository.ReportScheduleRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages the recurring report schedules of an organization. */
public class ReportScheduleService {

  private final ReportScheduleRepository scheduleRepository;

  /** Wires the service with its repository. */
  public ReportScheduleService(ReportScheduleRepository scheduleRepository) {
    this.scheduleRepository = scheduleRepository;
  }

  /** Creates a new enabled report schedule. */
  @Transactional
  public Mono<ReportSchedule> createSchedule(
      UUID organizationId,
      ReportType type,
      String cronExpression,
      ReportFormat format,
      String recipients,
      String parameters,
      Instant nextRunAt,
      UUID createdBy) {
    return scheduleRepository.save(
        new ReportSchedule(
            organizationId,
            type,
            cronExpression,
            format,
            recipients,
            parameters,
            nextRunAt,
            createdBy));
  }

  /** Returns a single live schedule of the organization. */
  @Transactional(readOnly = true)
  public Mono<ReportSchedule> getSchedule(UUID scheduleId, UUID organizationId) {
    return scheduleRepository
        .findLiveById(scheduleId)
        .switchIfEmpty(Mono.error(new NotFoundException("Report schedule not found")))
        .flatMap(
            schedule -> {
              if (!organizationId.equals(schedule.getOrganizationId())) {
                return Mono.error(new NotFoundException("Report schedule not found"));
              }
              return Mono.just(schedule);
            });
  }

  /** Lists the live schedules of an organization. */
  @Transactional(readOnly = true)
  public Flux<ReportSchedule> listSchedules(UUID organizationId) {
    return scheduleRepository.listLiveByOrganization(organizationId);
  }

  /** Updates a schedule definition. */
  @Transactional
  public Mono<ReportSchedule> updateSchedule(
      UUID scheduleId,
      UUID organizationId,
      String cronExpression,
      ReportFormat format,
      String recipients,
      String parameters,
      Instant nextRunAt,
      UUID byUser) {
    return getSchedule(scheduleId, organizationId)
        .map(
            schedule -> {
              schedule.update(cronExpression, format, recipients, parameters, nextRunAt, byUser);
              return schedule;
            })
        .flatMap(scheduleRepository::save);
  }

  /** Enables a schedule. */
  @Transactional
  public Mono<ReportSchedule> enableSchedule(UUID scheduleId, UUID organizationId, UUID byUser) {
    return getSchedule(scheduleId, organizationId)
        .map(
            schedule -> {
              schedule.enable(byUser);
              return schedule;
            })
        .flatMap(scheduleRepository::save);
  }

  /** Disables a schedule. */
  @Transactional
  public Mono<ReportSchedule> disableSchedule(UUID scheduleId, UUID organizationId, UUID byUser) {
    return getSchedule(scheduleId, organizationId)
        .map(
            schedule -> {
              schedule.disable(byUser);
              return schedule;
            })
        .flatMap(scheduleRepository::save);
  }

  /** Soft deletes a schedule. */
  @Transactional
  public Mono<Void> deleteSchedule(UUID scheduleId, UUID organizationId, UUID byUser) {
    return getSchedule(scheduleId, organizationId)
        .flatMap(
            schedule -> {
              schedule.delete(byUser);
              return scheduleRepository.save(schedule).then();
            });
  }
}
