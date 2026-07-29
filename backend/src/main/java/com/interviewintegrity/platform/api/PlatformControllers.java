package com.interviewintegrity.platform.api;

import com.interviewintegrity.platform.api.ApiDtos.AuthRequest;
import com.interviewintegrity.platform.api.ApiDtos.AuthResponse;
import com.interviewintegrity.platform.api.ApiDtos.InterviewCreateRequest;
import com.interviewintegrity.platform.api.ApiDtos.NotificationRequest;
import com.interviewintegrity.platform.api.ApiDtos.ReportResponse;
import com.interviewintegrity.platform.api.ApiDtos.SessionStartRequest;
import com.interviewintegrity.platform.api.ApiDtos.TelemetryIngestRequest;
import com.interviewintegrity.platform.api.ApiDtos.ViolationResponse;
import com.interviewintegrity.platform.application.PlatformServices.ReportService;
import com.interviewintegrity.platform.application.PlatformServices.TokenIssuer;
import com.interviewintegrity.platform.application.PlatformServices.TelemetryCommandService;
import com.interviewintegrity.platform.domain.DomainModel.Interview;
import com.interviewintegrity.platform.domain.DomainModel.InterviewSession;
import com.interviewintegrity.platform.domain.DomainModel.InterviewStatus;
import com.interviewintegrity.platform.domain.DomainModel.SessionStatus;
import com.interviewintegrity.platform.infrastructure.Repositories.InterviewRepository;
import com.interviewintegrity.platform.infrastructure.Repositories.InterviewSessionRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class PlatformControllers {
  private PlatformControllers() {}

  @RestController
  @RequestMapping("/api/v1/auth")
  public static class AuthController {
    private final TokenIssuer tokenIssuer;

    public AuthController(TokenIssuer tokenIssuer) {
      this.tokenIssuer = tokenIssuer;
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
      return tokenIssuer.issue(request.email(), request.deviceId());
    }
  }

  @RestController
  @RequestMapping("/api/v1/interviews")
  public static class InterviewController {
    private final InterviewRepository interviews;

    public InterviewController(InterviewRepository interviews) {
      this.interviews = interviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Interview> create(@Valid @RequestBody InterviewCreateRequest request) {
      Interview interview = new Interview();
      interview.id = UUID.randomUUID();
      interview.companyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
      interview.candidateId = request.candidateId();
      interview.recruiterId = request.recruiterId();
      interview.meetingUrl = request.meetingUrl();
      interview.startsAt = request.startsAt();
      interview.status = InterviewStatus.SCHEDULED;
      return interviews.save(interview);
    }

    @GetMapping("/recruiter/{recruiterId}")
    public Flux<Interview> recruiterQueue(@PathVariable UUID recruiterId) {
      return interviews.findByRecruiterId(recruiterId);
    }
  }

  @RestController
  @RequestMapping("/api/v1/sessions")
  public static class SessionController {
    private final InterviewSessionRepository sessions;

    public SessionController(InterviewSessionRepository sessions) {
      this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InterviewSession> start(@Valid @RequestBody SessionStartRequest request) {
      InterviewSession session = new InterviewSession();
      session.id = UUID.randomUUID();
      session.interviewId = request.interviewId();
      session.candidateId = UUID.fromString("00000000-0000-0000-0000-000000000002");
      session.deviceId = request.deviceId();
      session.status = SessionStatus.AUTHENTICATED;
      session.lastHeartbeatAt = Instant.now();
      return sessions.save(session);
    }
  }

  @RestController
  @RequestMapping("/api/v1/telemetry")
  public static class TelemetryController {
    private final TelemetryCommandService telemetry;

    public TelemetryController(TelemetryCommandService telemetry) {
      this.telemetry = telemetry;
    }

    @PostMapping
    public Mono<java.util.List<ViolationResponse>> ingest(@Valid @RequestBody TelemetryIngestRequest request) {
      return telemetry.ingest(request);
    }
  }

  @RestController
  @RequestMapping("/api/v1/reports")
  public static class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
      this.reports = reports;
    }

    @GetMapping("/sessions/{sessionId}")
    public Mono<ReportResponse> session(@PathVariable UUID sessionId) {
      return reports.buildSessionReport(sessionId);
    }
  }

  @RestController
  @RequestMapping("/api/v1/notifications")
  public static class NotificationController {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Map<String, String>> send(@Valid @RequestBody NotificationRequest request) {
      return Mono.just(Map.of("status", "queued", "channel", request.channel()));
    }
  }
}
