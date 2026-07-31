package com.interviewintegrity.platform.infrastructure;

import com.interviewintegrity.platform.domain.DomainModel.Company;
import com.interviewintegrity.platform.domain.DomainModel.Interview;
import com.interviewintegrity.platform.domain.DomainModel.InterviewSession;
import com.interviewintegrity.platform.domain.DomainModel.Policy;
import com.interviewintegrity.platform.domain.DomainModel.TelemetryEvent;
import com.interviewintegrity.platform.domain.DomainModel.User;
import com.interviewintegrity.platform.domain.DomainModel.Violation;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class Repositories {
  private Repositories() {}

  public interface CompanyRepository extends ReactiveCrudRepository<Company, UUID> {}

  public interface UserRepository extends ReactiveCrudRepository<User, UUID> {}

  public interface InterviewRepository extends ReactiveCrudRepository<Interview, UUID> {
    Flux<Interview> findByRecruiterId(UUID recruiterId);

    Flux<Interview> findByCandidateId(UUID candidateId);
  }

  public interface InterviewSessionRepository
      extends ReactiveCrudRepository<InterviewSession, UUID> {
    Flux<InterviewSession> findByInterviewId(UUID interviewId);
  }

  public interface TelemetryEventRepository extends ReactiveCrudRepository<TelemetryEvent, UUID> {
    Flux<TelemetryEvent> findTop100BySessionIdOrderByOccurredAtDesc(UUID sessionId);
  }

  public interface PolicyRepository extends ReactiveCrudRepository<Policy, UUID> {
    Flux<Policy> findByCompanyIdAndEnabledTrue(UUID companyId);
  }

  public interface ViolationRepository extends ReactiveCrudRepository<Violation, UUID> {
    Flux<Violation> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
  }
}
