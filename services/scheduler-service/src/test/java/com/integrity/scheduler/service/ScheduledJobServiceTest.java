package com.integrity.scheduler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.integrity.exception.NotFoundException;
import com.integrity.scheduler.domain.JobStatus;
import com.integrity.scheduler.domain.ScheduledJob;
import com.integrity.scheduler.repository.JobLockRepository;
import com.integrity.scheduler.repository.ScheduledJobRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the scheduled job service. */
@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

  @Mock private ScheduledJobRepository jobRepository;
  @Mock private JobLockRepository lockRepository;

  private ScheduledJobService jobService;

  @BeforeEach
  void setUp() {
    jobService = new ScheduledJobService(jobRepository, lockRepository);
  }

  @Test
  void createJobSavesAsEnabled() {
    UUID organizationId = UUID.randomUUID();
    when(jobRepository.save(any(ScheduledJob.class)))
        .thenAnswer(
            invocation -> {
              ScheduledJob job = invocation.getArgument(0);
              job.setId(UUID.randomUUID());
              return Mono.just(job);
            });

    StepVerifier.create(
            jobService.createJob(
                organizationId,
                "Daily cleanup",
                "cleanup",
                "0 0 * * *",
                "cleanupHandler",
                "{\"retention\":30}",
                2,
                600,
                UUID.randomUUID()))
        .assertNext(
            job -> {
              org.assertj.core.api.Assertions.assertThat(job.getStatus())
                  .isEqualTo(JobStatus.ENABLED);
              org.assertj.core.api.Assertions.assertThat(job.getOrganizationId())
                  .isEqualTo(organizationId);
              org.assertj.core.api.Assertions.assertThat(job.getMaxRetries()).isEqualTo(2);
            })
        .verifyComplete();
  }

  @Test
  void pauseJobMarksItPaused() {
    UUID organizationId = UUID.randomUUID();
    ScheduledJob job =
        new ScheduledJob(
            organizationId,
            "Daily cleanup",
            "cleanup",
            "0 0 * * *",
            "cleanupHandler",
            null,
            0,
            300,
            null);
    job.setId(UUID.randomUUID());

    when(jobRepository.findLiveByIdAndOrganization(job.getId(), organizationId))
        .thenReturn(Mono.just(job));
    when(jobRepository.save(any(ScheduledJob.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(jobService.pauseJob(job.getId(), organizationId, UUID.randomUUID()))
        .assertNext(
            paused ->
                org.assertj.core.api.Assertions.assertThat(paused.getStatus())
                    .isEqualTo(JobStatus.PAUSED))
        .verifyComplete();
  }

  @Test
  void resumeJobMarksItEnabledAgain() {
    UUID organizationId = UUID.randomUUID();
    ScheduledJob job =
        new ScheduledJob(
            organizationId,
            "Daily cleanup",
            "cleanup",
            "0 0 * * *",
            "cleanupHandler",
            null,
            0,
            300,
            null);
    job.setId(UUID.randomUUID());

    when(jobRepository.findLiveByIdAndOrganization(job.getId(), organizationId))
        .thenReturn(Mono.just(job));
    when(jobRepository.save(any(ScheduledJob.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(jobService.resumeJob(job.getId(), organizationId, UUID.randomUUID()))
        .assertNext(
            resumed ->
                org.assertj.core.api.Assertions.assertThat(resumed.getStatus())
                    .isEqualTo(JobStatus.ENABLED))
        .verifyComplete();
  }

  @Test
  void runDueAdvancesJobUnderLock() {
    UUID organizationId = UUID.randomUUID();
    ScheduledJob job =
        new ScheduledJob(
            organizationId,
            "Daily cleanup",
            "cleanup",
            "0 0 * * *",
            "cleanupHandler",
            null,
            0,
            300,
            null);
    job.setId(UUID.randomUUID());

    when(jobRepository.listDueByOrganization(
            org.mockito.ArgumentMatchers.eq(organizationId), any(Instant.class)))
        .thenReturn(Flux.just(job));
    when(lockRepository.tryAcquire(
            org.mockito.ArgumentMatchers.eq(job.getId()),
            any(String.class),
            any(String.class),
            any(Instant.class)))
        .thenReturn(Mono.just(true));
    when(jobRepository.save(any(ScheduledJob.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(lockRepository.release(org.mockito.ArgumentMatchers.eq(job.getId()), any(String.class)))
        .thenReturn(Mono.just(true));

    StepVerifier.create(jobService.runDue(organizationId))
        .assertNext(
            run -> {
              org.assertj.core.api.Assertions.assertThat(run.getLastRunAt()).isNotNull();
              org.assertj.core.api.Assertions.assertThat(run.getNextRunAt()).isNull();
            })
        .verifyComplete();

    verify(jobRepository).save(any(ScheduledJob.class));
    verify(lockRepository).release(org.mockito.ArgumentMatchers.eq(job.getId()), any(String.class));
  }

  @Test
  void runDueSkipsJobWhenLockIsHeld() {
    UUID organizationId = UUID.randomUUID();
    ScheduledJob job =
        new ScheduledJob(
            organizationId,
            "Daily cleanup",
            "cleanup",
            "0 0 * * *",
            "cleanupHandler",
            null,
            0,
            300,
            null);
    job.setId(UUID.randomUUID());

    when(jobRepository.listDueByOrganization(
            org.mockito.ArgumentMatchers.eq(organizationId), any(Instant.class)))
        .thenReturn(Flux.just(job));
    when(lockRepository.tryAcquire(
            org.mockito.ArgumentMatchers.eq(job.getId()),
            any(String.class),
            any(String.class),
            any(Instant.class)))
        .thenReturn(Mono.just(false));

    StepVerifier.create(jobService.runDue(organizationId))
        .assertNext(
            run -> {
              org.assertj.core.api.Assertions.assertThat(run.getLastRunAt()).isNull();
              org.assertj.core.api.Assertions.assertThat(run.getNextRunAt()).isNull();
            })
        .verifyComplete();

    verify(jobRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void getJobReturnsNotFoundForUnknownJob() {
    UUID jobId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    when(jobRepository.findLiveByIdAndOrganization(jobId, organizationId)).thenReturn(Mono.empty());

    StepVerifier.create(jobService.getJob(jobId, organizationId))
        .expectError(NotFoundException.class)
        .verify();
  }
}
