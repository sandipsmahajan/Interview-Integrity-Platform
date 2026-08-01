package com.interviewintegrity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.RateLimitException;
import com.interviewintegrity.identity.domain.OtpCode;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.repository.OtpCodeRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the email one-time passcode service. */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

  private static final String PURPOSE = "mfa-login";

  @Mock private OtpCodeRepository otpCodeRepository;
  @Mock private EmailEventPublisher emailEventPublisher;

  private OtpService otpService;

  @BeforeEach
  void setUp() {
    otpService = new OtpService(otpCodeRepository, emailEventPublisher);
  }

  @Test
  void requestSavesHashedCodeAndPublishesEmail() {
    User user = user();
    when(otpCodeRepository.countRequestedSince(eq(user.getId()), eq(PURPOSE), any()))
        .thenReturn(Mono.just(0L));
    when(otpCodeRepository.save(any(OtpCode.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(emailEventPublisher.publish(any(IdentityEmailEvent.class))).thenReturn(Mono.empty());

    StepVerifier.create(otpService.request(user, PURPOSE)).verifyComplete();

    ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
    verify(otpCodeRepository).save(otpCaptor.capture());
    OtpCode saved = otpCaptor.getValue();
    assertThat(saved.getCodeHash()).matches("^[0-9a-f]{64}$");

    ArgumentCaptor<IdentityEmailEvent> eventCaptor =
        ArgumentCaptor.forClass(IdentityEmailEvent.class);
    verify(emailEventPublisher).publish(eventCaptor.capture());
    String code = eventCaptor.getValue().templateData().get("otpCode");
    assertThat(code).matches("^\\d{6}$");
    assertThat(saved.getCodeHash()).isNotEqualTo(code);
  }

  @Test
  void requestRejectsResendWithinRateWindow() {
    User user = user();
    when(otpCodeRepository.countRequestedSince(eq(user.getId()), eq(PURPOSE), any()))
        .thenReturn(Mono.just(1L));

    StepVerifier.create(otpService.request(user, PURPOSE))
        .expectError(RateLimitException.class)
        .verify();
  }

  @Test
  void verifyConsumesValidCode() {
    User user = user();
    OtpCode otp =
        new OtpCode(
            user.getId(),
            user.getOrganizationId(),
            PURPOSE,
            "hash",
            5,
            Instant.now().plusSeconds(600));
    when(otpCodeRepository.findOutstandingByHash(any(), any(), any())).thenReturn(Mono.just(otp));
    when(otpCodeRepository.save(any(OtpCode.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(otpService.verify(user, PURPOSE, "123456")).verifyComplete();

    assertThat(otp.getConsumedAt()).isNotNull();
    verify(otpCodeRepository).save(otp);
  }

  @Test
  void verifyRejectsMalformedCodeWithoutRepositoryCall() {
    User user = user();

    StepVerifier.create(otpService.verify(user, PURPOSE, "12ab6"))
        .expectError(AuthenticationFailedException.class)
        .verify();
    verify(otpCodeRepository, never()).findOutstandingByHash(any(), any(), any());
  }

  @Test
  void verifyRejectsUnknownCodeAndRecordsFailedAttempt() {
    User user = user();
    OtpCode otp =
        new OtpCode(
            user.getId(),
            user.getOrganizationId(),
            PURPOSE,
            "hash",
            5,
            Instant.now().plusSeconds(600));
    when(otpCodeRepository.findOutstandingByHash(any(), any(), any())).thenReturn(Mono.empty());
    when(otpCodeRepository.findOutstanding(user.getId(), PURPOSE)).thenReturn(Mono.just(otp));
    when(otpCodeRepository.save(any(OtpCode.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(otpService.verify(user, PURPOSE, "000000"))
        .expectError(AuthenticationFailedException.class)
        .verify();

    assertThat(otp.getAttempts()).isEqualTo(1);
  }

  @Test
  void verifyRejectsCodePastMaxAttempts() {
    User user = user();
    OtpCode otp =
        new OtpCode(
            user.getId(),
            user.getOrganizationId(),
            PURPOSE,
            "hash",
            1,
            Instant.now().plusSeconds(600));
    otp.recordAttempt();
    when(otpCodeRepository.findOutstandingByHash(any(), any(), any())).thenReturn(Mono.just(otp));
    when(otpCodeRepository.findOutstanding(user.getId(), PURPOSE)).thenReturn(Mono.empty());

    StepVerifier.create(otpService.verify(user, PURPOSE, "123456"))
        .expectError(AuthenticationFailedException.class)
        .verify();
  }

  private static User user() {
    User user = new User(UUID.randomUUID(), "alice@example.com", "hash", "Alice");
    user.setId(UUID.randomUUID());
    return user;
  }
}
