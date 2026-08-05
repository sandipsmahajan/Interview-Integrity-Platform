package com.integrity.recruiter.service;

import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import com.integrity.recruiter.domain.CandidatePipeline;
import com.integrity.recruiter.domain.PipelineStage;
import com.integrity.recruiter.domain.PipelineStatus;
import com.integrity.recruiter.repository.CandidatePipelineRepository;
import com.integrity.recruiter.repository.PipelineStageRepository;
import com.integrity.recruiter.repository.RecruiterRepository;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages pipeline stages and candidate movement through the hiring pipeline. */
public class PipelineService {

  private static final String PIPELINE_STAGE_NOT_FOUND = "Pipeline stage not found";

  private final PipelineStageRepository stageRepository;
  private final CandidatePipelineRepository pipelineRepository;
  private final RecruiterRepository recruiterRepository;

  /** Wires the service with its repositories. */
  public PipelineService(
      PipelineStageRepository stageRepository,
      CandidatePipelineRepository pipelineRepository,
      RecruiterRepository recruiterRepository) {
    this.stageRepository = stageRepository;
    this.pipelineRepository = pipelineRepository;
    this.recruiterRepository = recruiterRepository;
  }

  /** Creates a pipeline stage, rejecting duplicate codes. */
  @Transactional
  public Mono<PipelineStage> createStage(
      UUID organizationId, String code, String name, int orderIndex, UUID createdBy) {
    return stageRepository
        .existsByOrganizationAndCode(organizationId, code)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Stage code already exists"));
              }
              return stageRepository.save(
                  new PipelineStage(organizationId, code, name, orderIndex, createdBy));
            });
  }

  /** Lists the stages of an organization. */
  @Transactional(readOnly = true)
  public Flux<PipelineStage> listStages(UUID organizationId) {
    return stageRepository.listLiveByOrganization(organizationId);
  }

  /** Updates a stage. */
  @Transactional
  public Mono<PipelineStage> updateStage(
      UUID stageId, UUID organizationId, String name, int orderIndex, UUID byUser) {
    return stageRepository
        .findLiveById(stageId)
        .switchIfEmpty(Mono.error(new NotFoundException(PIPELINE_STAGE_NOT_FOUND)))
        .flatMap(stage -> assertStageOrganization(stage, organizationId))
        .map(
            stage -> {
              stage.update(name, orderIndex, byUser);
              return stage;
            })
        .flatMap(stageRepository::save);
  }

  /** Soft deletes a stage. */
  @Transactional
  public Mono<Void> deleteStage(UUID stageId, UUID organizationId, UUID byUser) {
    return stageRepository
        .findLiveById(stageId)
        .switchIfEmpty(Mono.error(new NotFoundException(PIPELINE_STAGE_NOT_FOUND)))
        .flatMap(stage -> assertStageOrganization(stage, organizationId))
        .map(
            stage -> {
              stage.delete(byUser);
              return stage;
            })
        .flatMap(stageRepository::save)
        .then();
  }

  /** Enters a candidate into a stage, exiting any previous current stage first. */
  @Transactional
  public Mono<CandidatePipeline> enterStage(
      UUID organizationId, UUID candidateId, UUID userId, UUID stageId, int position) {
    return stageRepository
        .findLiveById(stageId)
        .switchIfEmpty(Mono.error(new NotFoundException(PIPELINE_STAGE_NOT_FOUND)))
        .flatMap(stage -> assertStageOrganization(stage, organizationId))
        .then(recruiterRepository.findLiveByOrganizationAndUser(organizationId, userId))
        .switchIfEmpty(Mono.error(new NotFoundException("Recruiter profile not found")))
        .flatMap(
            recruiter ->
                pipelineRepository
                    .findCurrentByStage(
                        organizationId, candidateId, stageId, PipelineStatus.CURRENT)
                    .hasElement()
                    .flatMap(
                        alreadyInStage -> {
                          if (alreadyInStage) {
                            return Mono.error(
                                new ConflictException("Candidate already in this stage"));
                          }
                          return pipelineRepository.save(
                              new CandidatePipeline(
                                  organizationId,
                                  candidateId,
                                  recruiter.getId(),
                                  stageId,
                                  position,
                                  userId));
                        }));
  }

  /** Lists the current entries of a stage. */
  @Transactional(readOnly = true)
  public Flux<CandidatePipeline> listStageCandidates(UUID organizationId, UUID stageId) {
    return pipelineRepository.listCurrentByStage(organizationId, stageId);
  }

  /** Lists the pipeline history of a candidate. */
  @Transactional(readOnly = true)
  public Flux<CandidatePipeline> listByCandidate(UUID organizationId, UUID candidateId) {
    return pipelineRepository.listByOrganizationAndCandidate(organizationId, candidateId);
  }

  /** Moves a candidate out of the current stage. */
  @Transactional
  public Mono<CandidatePipeline> exitStage(UUID organizationId, UUID candidateId, UUID stageId) {
    return pipelineRepository
        .findCurrentByStage(organizationId, candidateId, stageId, PipelineStatus.CURRENT)
        .switchIfEmpty(Mono.error(new NotFoundException("Candidate not in this stage")))
        .map(CandidatePipeline::exit)
        .flatMap(pipelineRepository::save);
  }

  private Mono<PipelineStage> assertStageOrganization(PipelineStage stage, UUID organizationId) {
    if (!organizationId.equals(stage.getOrganizationId())) {
      return Mono.error(new NotFoundException(PIPELINE_STAGE_NOT_FOUND));
    }
    return Mono.just(stage);
  }
}
