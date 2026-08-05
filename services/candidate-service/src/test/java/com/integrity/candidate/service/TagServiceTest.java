package com.integrity.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.integrity.candidate.domain.Candidate;
import com.integrity.candidate.domain.Tag;
import com.integrity.candidate.repository.CandidateTagRepository;
import com.integrity.candidate.repository.TagRepository;
import com.integrity.exception.ConflictException;
import com.integrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the tag service. */
class TagServiceTest {

  private static final String TAG_CODE = "vip";
  private static final String TAG_NAME = "VIP";

  private final TagRepository tagRepository = Mockito.mock(TagRepository.class);
  private final CandidateTagRepository candidateTagRepository =
      Mockito.mock(CandidateTagRepository.class);
  private final CandidateService candidateService = Mockito.mock(CandidateService.class);

  private TagService tagService;

  @BeforeEach
  void setUp() {
    tagService = new TagService(tagRepository, candidateTagRepository, candidateService);
  }

  @Test
  void createRejectsDuplicateCode() {
    UUID organizationId = UUID.randomUUID();
    when(tagRepository.existsByOrganizationAndCode(organizationId, TAG_CODE))
        .thenReturn(Mono.just(true));

    StepVerifier.create(tagService.create(organizationId, TAG_CODE, TAG_NAME))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void attachAppliesTagToCandidate() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID tagId = UUID.randomUUID();
    UUID taggedBy = UUID.randomUUID();
    Tag tag = new Tag(organizationId, TAG_CODE, TAG_NAME);
    tag.setId(tagId);
    when(tagRepository.findById(tagId)).thenReturn(Mono.just(tag));
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(candidateTagRepository.add(candidateId, tagId, taggedBy)).thenReturn(Mono.empty());

    StepVerifier.create(tagService.attach(organizationId, candidateId, tagId, taggedBy))
        .assertNext(attached -> assertThat(attached.getCode()).isEqualTo(TAG_CODE))
        .verifyComplete();
  }

  @Test
  void attachRejectsCrossTenantTag() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID tagId = UUID.randomUUID();
    Tag tag = new Tag(UUID.randomUUID(), TAG_CODE, TAG_NAME);
    tag.setId(tagId);
    when(tagRepository.findById(tagId)).thenReturn(Mono.just(tag));

    StepVerifier.create(tagService.attach(organizationId, candidateId, tagId, UUID.randomUUID()))
        .expectError(NotFoundException.class)
        .verify();
  }

  @Test
  void detachRemovesTagFromCandidate() {
    UUID organizationId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID tagId = UUID.randomUUID();
    Tag tag = new Tag(organizationId, TAG_CODE, TAG_NAME);
    tag.setId(tagId);
    when(tagRepository.findById(tagId)).thenReturn(Mono.just(tag));
    when(candidateService.requireCandidate(candidateId, organizationId))
        .thenReturn(Mono.just(candidate(candidateId, organizationId)));
    when(candidateTagRepository.remove(candidateId, tagId)).thenReturn(Mono.empty());

    StepVerifier.create(tagService.detach(organizationId, candidateId, tagId)).verifyComplete();
  }

  private static Candidate candidate(UUID candidateId, UUID organizationId) {
    Candidate candidate =
        new Candidate(organizationId, null, "a@b.com", "Jane", null, null, UUID.randomUUID());
    candidate.setId(candidateId);
    return candidate;
  }
}
