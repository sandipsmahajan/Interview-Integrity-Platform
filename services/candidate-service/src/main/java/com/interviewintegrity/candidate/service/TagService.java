package com.interviewintegrity.candidate.service;

import com.interviewintegrity.candidate.domain.Tag;
import com.interviewintegrity.candidate.repository.CandidateTagRepository;
import com.interviewintegrity.candidate.repository.TagRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages tags and their application to candidates. */
public class TagService {

  private final TagRepository tagRepository;
  private final CandidateTagRepository candidateTagRepository;
  private final CandidateService candidateService;

  /** Wires the service with its repositories and the candidate service. */
  public TagService(
      TagRepository tagRepository,
      CandidateTagRepository candidateTagRepository,
      CandidateService candidateService) {
    this.tagRepository = tagRepository;
    this.candidateTagRepository = candidateTagRepository;
    this.candidateService = candidateService;
  }

  /** Creates a tag, rejecting duplicate codes within the organization. */
  @Transactional
  public Mono<Tag> create(UUID organizationId, String code, String name) {
    return tagRepository
        .existsByOrganizationAndCode(organizationId, code)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new ConflictException("Tag code already exists"));
              }
              return tagRepository.save(new Tag(organizationId, code.trim(), name.trim()));
            });
  }

  /** Lists the tags of an organization. */
  @Transactional(readOnly = true)
  public Flux<Tag> list(UUID organizationId) {
    return tagRepository.listByOrganization(organizationId);
  }

  /** Applies a tag to a candidate. */
  @Transactional
  public Mono<Tag> attach(UUID organizationId, UUID candidateId, UUID tagId, UUID taggedBy) {
    return requireTag(organizationId, tagId)
        .flatMap(
            tag -> candidateService.requireCandidate(candidateId, organizationId).thenReturn(tag))
        .flatMap(tag -> candidateTagRepository.add(candidateId, tagId, taggedBy).thenReturn(tag));
  }

  /** Removes a tag from a candidate. */
  @Transactional
  public Mono<Void> detach(UUID organizationId, UUID candidateId, UUID tagId) {
    return requireTag(organizationId, tagId)
        .then(candidateService.requireCandidate(candidateId, organizationId))
        .then(candidateTagRepository.remove(candidateId, tagId));
  }

  /** Lists the tags applied to a candidate. */
  @Transactional(readOnly = true)
  public Flux<Tag> listByCandidate(UUID organizationId, UUID candidateId) {
    return candidateService
        .requireCandidate(candidateId, organizationId)
        .thenMany(candidateTagRepository.listTagsByCandidate(candidateId));
  }

  private Mono<Tag> requireTag(UUID organizationId, UUID tagId) {
    return tagRepository
        .findById(tagId)
        .switchIfEmpty(Mono.error(new NotFoundException("Tag not found")))
        .flatMap(
            tag -> {
              if (!organizationId.equals(tag.getOrganizationId())) {
                return Mono.error(new NotFoundException("Tag not found"));
              }
              return Mono.just(tag);
            });
  }
}
