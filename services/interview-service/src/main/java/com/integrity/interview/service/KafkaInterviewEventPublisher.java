package com.integrity.interview.service;

import com.integrity.event.EventEnvelope;
import com.integrity.event.IdentityEmailEvent;
import com.integrity.event.InterviewCompletedEvent;
import com.integrity.event.InterviewCreatedEvent;
import com.integrity.event.InterviewScheduledEvent;
import com.integrity.event.InterviewStartedEvent;
import com.integrity.event.KafkaTopics;
import com.integrity.interview.domain.Interview;
import com.integrity.interview.domain.InterviewSession;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka backed {@link InterviewEventPublisher}.
 *
 * <p>Each event is wrapped in the platform {@link EventEnvelope} and serialized to JSON before
 * being sent to the topic partitioned by organization id.
 */
public final class KafkaInterviewEventPublisher implements InterviewEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaInterviewEventPublisher.class);

  private final KafkaSender<String, String> sender;
  private final ObjectMapper objectMapper;
  private final String serviceName;

  /** Creates a publisher bound to the given sender. */
  public KafkaInterviewEventPublisher(KafkaSender<String, String> sender, String serviceName) {
    this.sender = sender;
    this.serviceName = serviceName;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Mono<Void> publishCreated(Interview interview) {
    Instant occurredAt = Instant.now();
    InterviewCreatedEvent payload =
        new InterviewCreatedEvent(
            interview.getId(),
            interview.getOrganizationId(),
            interview.getCandidateId(),
            interview.getRecruiterId(),
            interview.getStartsAt(),
            occurredAt);
    return publish(
        KafkaTopics.INTERVIEW_CREATED, payload, interview.getOrganizationId(), occurredAt);
  }

  @Override
  public Mono<Void> publishScheduled(Interview interview) {
    Instant occurredAt = Instant.now();
    InterviewScheduledEvent payload =
        new InterviewScheduledEvent(
            interview.getId(),
            interview.getOrganizationId(),
            interview.getCandidateId(),
            interview.getRecruiterId(),
            interview.getStartsAt(),
            occurredAt);
    return publish(
        KafkaTopics.INTERVIEW_SCHEDULED, payload, interview.getOrganizationId(), occurredAt);
  }

  @Override
  public Mono<Void> publishStarted(InterviewSession session) {
    Instant occurredAt = Instant.now();
    InterviewStartedEvent payload =
        new InterviewStartedEvent(session.getInterviewId(), session.getId(), occurredAt);
    return publish(KafkaTopics.INTERVIEW_STARTED, payload, session.getOrganizationId(), occurredAt);
  }

  @Override
  public Mono<Void> publishCompleted(InterviewSession session) {
    Instant occurredAt = Instant.now();
    InterviewCompletedEvent payload =
        new InterviewCompletedEvent(session.getInterviewId(), session.getId(), occurredAt);
    return publish(
        KafkaTopics.INTERVIEW_COMPLETED, payload, session.getOrganizationId(), occurredAt);
  }

  @Override
  public Mono<Void> publishCandidateInvitation(
      Interview interview, String candidateEmail, String candidateName, String downloadUrl) {
    Instant occurredAt = Instant.now();
    String tempPassword = generateTempPassword();
    String displayName = candidateName != null ? candidateName : "Candidate";

    DateTimeFormatter dateFmt =
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a")
            .withZone(ZoneId.of(interview.getTimezone()));

    IdentityEmailEvent payload =
        new IdentityEmailEvent(
            interview.getCandidateId(),
            interview.getOrganizationId(),
            candidateEmail,
            displayName,
            "en",
            "interview-invitation",
            Map.of(
                "candidateName",
                displayName,
                "interviewTitle",
                interview.getTitle(),
                "interviewDate",
                dateFmt.format(interview.getStartsAt()),
                "interviewMode",
                interview.getMode().name(),
                "meetingUrl",
                interview.getMeetingUrl() != null ? interview.getMeetingUrl() : "",
                "tempPassword",
                tempPassword,
                "downloadUrl",
                downloadUrl,
                "appName",
                "Integrity Pro"),
            occurredAt);

    return publishEmail(payload, interview.getOrganizationId(), occurredAt);
  }

  private Mono<Void> publishEmail(
      IdentityEmailEvent event, UUID organizationId, Instant occurredAt) {
    EventEnvelope envelope =
        new EventEnvelope(
            UUID.randomUUID(), KafkaTopics.IDENTITY_EMAIL, serviceName, occurredAt, toJson(event));
    String key = organizationId.toString();
    ProducerRecord<String, String> record =
        new ProducerRecord<>(KafkaTopics.IDENTITY_EMAIL, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published candidate invitation for {} to topic {}",
                    event.email(),
                    KafkaTopics.IDENTITY_EMAIL))
        .then();
  }

  private static String generateTempPassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    SecureRandom random = new SecureRandom();
    return IntStream.range(0, 12)
        .map(i -> random.nextInt(chars.length()))
        .mapToObj(chars::charAt)
        .map(Object::toString)
        .collect(Collectors.joining());
  }

  private Mono<Void> publish(
      String topic, Object payload, UUID organizationId, Instant occurredAt) {
    EventEnvelope envelope =
        new EventEnvelope(UUID.randomUUID(), topic, serviceName, occurredAt, toJson(payload));
    String key = organizationId.toString();
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, toJson(envelope));
    SenderRecord<String, String, String> senderRecord = SenderRecord.create(record, key);
    return sender
        .send(Mono.just(senderRecord))
        .doOnNext(
            result ->
                log.info(
                    "Published interview event for organization {} to topic {}",
                    organizationId,
                    result.recordMetadata().topic()))
        .then();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to serialize event payload", e);
    }
  }
}
