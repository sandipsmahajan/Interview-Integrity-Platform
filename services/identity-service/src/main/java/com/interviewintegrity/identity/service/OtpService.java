package com.interviewintegrity.identity.service;

import com.interviewintegrity.event.IdentityEmailEvent;
import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.RateLimitException;
import com.interviewintegrity.identity.domain.OtpCode;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.repository.OtpCodeRepository;
import com.interviewintegrity.security.RefreshTokens;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Email one-time passcode lifecycle.
 *
 * <p>Generates six digit codes, persists only their SHA-256 hash, enforces a minimum interval
 * between resends and a maximum number of verification attempts per code, and hands the plaintext
 * code to the notification service for email delivery.
 */
public final class OtpService {

  private static final Duration CODE_TTL = Duration.ofMinutes(10);
  private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
  private static final int MAX_ATTEMPTS = 5;
  private static final int CODE_DIGITS = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final OtpCodeRepository otpCodeRepository;
  private final EmailEventPublisher emailEventPublisher;

  /** Creates the OTP service with its collaborators. */
  public OtpService(OtpCodeRepository otpCodeRepository, EmailEventPublisher emailEventPublisher) {
    this.otpCodeRepository = otpCodeRepository;
    this.emailEventPublisher = emailEventPublisher;
  }

  /**
   * Requests a new code for the user and purpose.
   *
   * <p>When an outstanding code already exists it is replaced, which enforces the resend rate limit
   * per user and purpose.
   */
  public Mono<Void> request(User user, String purpose) {
    Instant since = Instant.now().minus(RESEND_INTERVAL);
    return otpCodeRepository
        .countRequestedSince(user.getId(), purpose, since)
        .flatMap(
            recent -> {
              if (recent > 0) {
                return Mono.error(
                    new RateLimitException("Please wait before requesting another code"));
              }
              String code = generateCode();
              OtpCode otp =
                  new OtpCode(
                      user.getId(),
                      user.getOrganizationId(),
                      purpose,
                      RefreshTokens.hash(code),
                      MAX_ATTEMPTS,
                      Instant.now().plus(CODE_TTL));
              return otpCodeRepository
                  .save(otp)
                  .then(
                      emailEventPublisher.publish(
                          new IdentityEmailEvent(
                              user.getId(),
                              user.getOrganizationId(),
                              user.getEmail(),
                              user.getDisplayName(),
                              Locale.ROOT.toLanguageTag(),
                              "email-otp",
                              Map.of(
                                  "otpCode",
                                  code,
                                  "expiresInMinutes",
                                  String.valueOf(CODE_TTL.toMinutes())),
                              Instant.now())));
            });
  }

  /** Verifies and consumes a code for the user and purpose. */
  public Mono<Void> verify(User user, String purpose, String code) {
    if (code == null
        || code.length() != CODE_DIGITS
        || !code.chars().allMatch(Character::isDigit)) {
      return Mono.error(new AuthenticationFailedException("Invalid code"));
    }
    String hash = RefreshTokens.hash(code);
    return otpCodeRepository
        .findOutstandingByHash(user.getId(), purpose, hash)
        .switchIfEmpty(Mono.error(new AuthenticationFailedException("Invalid or expired code")))
        .flatMap(
            otp -> {
              if (!otp.isUsable()) {
                return Mono.error(new AuthenticationFailedException("Invalid or expired code"));
              }
              otp.consume();
              return otpCodeRepository.save(otp).then();
            })
        .onErrorResume(err -> recordFailedAttempt(user, purpose).then(Mono.error(err)));
  }

  private Mono<Void> recordFailedAttempt(User user, String purpose) {
    return otpCodeRepository
        .findOutstanding(user.getId(), purpose)
        .flatMap(
            otp -> {
              otp.recordAttempt();
              return otpCodeRepository.save(otp).then();
            })
        .switchIfEmpty(Mono.empty());
  }

  private static String generateCode() {
    int value = RANDOM.nextInt(1_000_000);
    return String.format("%0" + CODE_DIGITS + "d", value);
  }
}
