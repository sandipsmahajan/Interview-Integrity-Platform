package com.interviewintegrity.identity.service;

import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.identity.config.AuthProperties;
import com.interviewintegrity.identity.domain.MfaDevice;
import com.interviewintegrity.identity.domain.RecoveryCode;
import com.interviewintegrity.identity.domain.TrustedDevice;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.repository.MfaChallengeAttemptRepository;
import com.interviewintegrity.identity.repository.MfaDeviceRepository;
import com.interviewintegrity.identity.repository.RecoveryCodeRepository;
import com.interviewintegrity.identity.repository.TrustedDeviceRepository;
import com.interviewintegrity.identity.repository.UserRepository;
import com.interviewintegrity.identity.web.dto.MfaChallengeResponse;
import com.interviewintegrity.identity.web.dto.MfaDeviceResponse;
import com.interviewintegrity.identity.web.dto.MfaEnrollResponse;
import com.interviewintegrity.identity.web.dto.TokenResponse;
import com.interviewintegrity.identity.web.dto.TrustedDeviceResponse;
import com.interviewintegrity.security.JwtTokenService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Multi-factor authentication flows.
 *
 * <p>Owns TOTP device enrollment and verification, recovery codes, trusted devices and the
 * challenge flow used to complete a login that requires a second factor.
 */
public final class MfaService {

  private static final String KIND_TOTP = "TOTP";
  private static final String PURPOSE_MFA_CHALLENGE = "mfa-challenge";
  private static final int TOTP_WINDOW = 1;
  private static final long NOT_FOUND = 0L;
  private static final int CONSUMED = 1;

  private final MfaDeviceRepository mfaDeviceRepository;
  private final RecoveryCodeRepository recoveryCodeRepository;
  private final TrustedDeviceRepository trustedDeviceRepository;
  private final UserRepository userRepository;
  private final TokenIssuer tokenIssuer;
  private final JwtTokenService jwtTokenService;
  private final OtpService otpService;
  private final MfaChallengeAttemptRepository challengeAttemptRepository;
  private final AuthProperties authProperties;

  /** Creates the MFA service with its collaborators. */
  public MfaService(
      MfaDeviceRepository mfaDeviceRepository,
      RecoveryCodeRepository recoveryCodeRepository,
      TrustedDeviceRepository trustedDeviceRepository,
      UserRepository userRepository,
      TokenIssuer tokenIssuer,
      JwtTokenService jwtTokenService,
      OtpService otpService,
      MfaChallengeAttemptRepository challengeAttemptRepository,
      AuthProperties authProperties) {
    this.mfaDeviceRepository = mfaDeviceRepository;
    this.recoveryCodeRepository = recoveryCodeRepository;
    this.trustedDeviceRepository = trustedDeviceRepository;
    this.userRepository = userRepository;
    this.tokenIssuer = tokenIssuer;
    this.jwtTokenService = jwtTokenService;
    this.otpService = otpService;
    this.challengeAttemptRepository = challengeAttemptRepository;
    this.authProperties = authProperties;
  }

  /** Returns true when the user has at least one verified MFA device. */
  public Mono<Boolean> hasVerifiedDevice(UUID userId) {
    return mfaDeviceRepository.findLiveVerifiedByUserIdAndKind(userId, KIND_TOTP).hasElement();
  }

  /** Returns true when the device is trusted to skip MFA challenges. */
  public Mono<Boolean> isTrustedDevice(UUID userId, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return Mono.just(false);
    }
    return trustedDeviceRepository.findByUserAndDeviceId(userId, deviceId).hasElement();
  }

  /** Issues a short-lived challenge the caller must satisfy with a second factor. */
  public Mono<MfaChallengeResponse> generateChallenge(User user) {
    String challengeId =
        jwtTokenService.issuePurposeToken(
            PURPOSE_MFA_CHALLENGE, user.getId(), authProperties.mfaChallengeTtl());
    List<String> channels = List.of("TOTP", "EMAIL", "RECOVERY");
    return Mono.just(
        new MfaChallengeResponse(
            true, challengeId, authProperties.mfaChallengeTtl().toSeconds(), channels));
  }

  /** Sends an email OTP for the user behind a pending challenge. */
  public Mono<Void> sendEmailOtp(String challengeId) {
    return resolveChallengeUser(challengeId)
        .flatMap(user -> otpService.request(user, authProperties.mfaEmailPurpose()));
  }

  /** Completes an MFA challenge and issues tokens, optionally trusting the device. */
  public Mono<TokenResponse> verifyChallenge(
      String challengeId,
      String code,
      boolean trustDevice,
      String deviceId,
      String deviceName,
      String ipAddress,
      String userAgent) {
    return resolveChallengeUser(challengeId)
        .flatMap(
            user ->
                assertChallengeBudget(challengeId)
                    .then(
                        verifyAnyFactor(user, code)
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(
                                Mono.defer(
                                    () ->
                                        challengeAttemptRepository
                                            .recordAttempt(challengeId, user.getId(), Instant.now())
                                            .then(
                                                Mono.error(
                                                    new AuthenticationFailedException(
                                                        "Invalid verification code")))))
                            .then(
                                challengeAttemptRepository
                                    .deleteByChallengeId(challengeId)
                                    .then(
                                        completeLogin(
                                            user,
                                            trustDevice,
                                            deviceId,
                                            deviceName,
                                            ipAddress,
                                            userAgent)))));
  }

  private Mono<Void> assertChallengeBudget(String challengeId) {
    return challengeAttemptRepository
        .attempts(challengeId)
        .defaultIfEmpty(0)
        .flatMap(
            attempts -> {
              if (attempts >= authProperties.maxMfaChallengeAttempts()) {
                return Mono.error(
                    new AuthenticationFailedException("Too many verification attempts"));
              }
              return Mono.empty();
            });
  }

  /** Starts a TOTP enrollment and issues a fresh set of recovery codes. */
  public Mono<MfaEnrollResponse> enrollTotp(User user) {
    return mfaDeviceRepository
        .findLiveVerifiedByUserIdAndKind(user.getId(), KIND_TOTP)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new ConflictException("A TOTP device is already verified for this user"));
              }
              return mfaDeviceRepository
                  .deletePendingByUserIdAndKind(user.getId(), KIND_TOTP)
                  .then(createPendingTotp(user))
                  .flatMap(
                      pending ->
                          recreateRecoveryCodes(user)
                              .map(
                                  codes ->
                                      new MfaEnrollResponse(
                                          pending.getSecretCiphertext(),
                                          Totp.otpauthUri(
                                              authProperties.appName(),
                                              user.getEmail(),
                                              pending.getSecretCiphertext()),
                                          codes)));
            });
  }

  /** Activates the pending TOTP device with a live code from the authenticator. */
  public Mono<Void> verifyTotpEnrollment(User user, String code) {
    return mfaDeviceRepository
        .findLivePendingByUserIdAndKind(user.getId(), KIND_TOTP)
        .switchIfEmpty(Mono.error(new AuthenticationFailedException("No pending TOTP enrollment")))
        .flatMap(
            device -> {
              if (!Totp.verify(device.getSecretCiphertext(), code, TOTP_WINDOW)) {
                return Mono.error(new AuthenticationFailedException("Invalid verification code"));
              }
              device.verify();
              return mfaDeviceRepository.save(device).then();
            });
  }

  /** Lists the MFA devices of the user. */
  public Mono<List<MfaDeviceResponse>> listDevices(UUID userId) {
    return mfaDeviceRepository
        .listLiveByUserId(userId)
        .map(
            device ->
                new MfaDeviceResponse(
                    device.getId(),
                    device.getKind(),
                    device.getVerifiedAt(),
                    device.getLastUsedAt()))
        .collectList();
  }

  /** Removes an MFA device of the user. */
  public Mono<Void> removeDevice(UUID userId, UUID deviceId) {
    return mfaDeviceRepository
        .findLiveByIdAndUserId(deviceId, userId)
        .switchIfEmpty(Mono.error(new NotFoundException("MFA device not found")))
        .flatMap(
            device -> {
              device.delete(userId);
              return mfaDeviceRepository.save(device).then();
            });
  }

  /** Regenerates the recovery code set, invalidating all previous codes. */
  public Mono<List<String>> regenerateRecoveryCodes(User user) {
    return recreateRecoveryCodes(user);
  }

  /** Lists the trusted devices of the user. */
  public Mono<List<TrustedDeviceResponse>> listTrustedDevices(UUID userId) {
    return trustedDeviceRepository
        .listByUser(userId)
        .map(
            device ->
                new TrustedDeviceResponse(
                    device.getId(),
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getLastSeenAt()))
        .collectList();
  }

  /** Removes a trusted device of the user. */
  public Mono<Void> removeTrustedDevice(UUID userId, UUID trustedDeviceId) {
    return trustedDeviceRepository
        .deleteByIdAndUser(trustedDeviceId, userId)
        .flatMap(
            affected -> {
              if (affected == NOT_FOUND) {
                return Mono.error(new NotFoundException("Trusted device not found"));
              }
              return Mono.empty();
            });
  }

  private Mono<MfaDevice> createPendingTotp(User user) {
    return mfaDeviceRepository.save(
        new MfaDevice(user.getId(), user.getOrganizationId(), KIND_TOTP, Totp.generateSecret()));
  }

  private Mono<List<String>> recreateRecoveryCodes(User user) {
    List<String> codes = RecoveryCodes.generate();
    return recoveryCodeRepository
        .deleteAllByUser(user.getId())
        .thenMany(Flux.fromIterable(codes))
        .concatMap(
            code ->
                recoveryCodeRepository.save(
                    new RecoveryCode(
                        user.getId(), user.getOrganizationId(), RecoveryCodes.hash(code))))
        .then(Mono.just(codes));
  }

  private Mono<User> resolveChallengeUser(String challengeId) {
    UUID userId = jwtTokenService.resolvePurposeToken(challengeId, PURPOSE_MFA_CHALLENGE);
    return userRepository
        .findLiveById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException("User no longer exists")));
  }

  private Mono<TokenResponse> completeLogin(
      User user,
      boolean trustDevice,
      String deviceId,
      String deviceName,
      String ipAddress,
      String userAgent) {
    user.markLoggedIn();
    Mono<Void> trust =
        trustDevice && deviceId != null && !deviceId.isBlank()
            ? trustDevice(user, deviceId, deviceName)
            : Mono.empty();
    return userRepository
        .save(user)
        .then(trust)
        .then(tokenIssuer.issue(user, deviceId, ipAddress, userAgent));
  }

  private Mono<Void> trustDevice(User user, String deviceId, String deviceName) {
    return trustedDeviceRepository
        .findByUserAndDeviceId(user.getId(), deviceId)
        .flatMap(
            device -> {
              device.touch();
              return trustedDeviceRepository.save(device).then();
            })
        .switchIfEmpty(
            Mono.defer(
                () ->
                    trustedDeviceRepository
                        .save(
                            new TrustedDevice(
                                user.getId(), user.getOrganizationId(), deviceId, deviceName))
                        .then()));
  }

  private Mono<Boolean> verifyAnyFactor(User user, String code) {
    return verifyTotp(user, code)
        .flatMap(matched -> matched ? Mono.just(true) : verifyEmailOtp(user, code))
        .flatMap(matched -> matched ? Mono.just(true) : verifyRecovery(user, code));
  }

  private Mono<Boolean> verifyTotp(User user, String code) {
    return mfaDeviceRepository
        .findLiveVerifiedByUserIdAndKind(user.getId(), KIND_TOTP)
        .flatMap(
            device -> {
              if (Totp.verify(device.getSecretCiphertext(), code, TOTP_WINDOW)) {
                device.markUsed();
                return mfaDeviceRepository.save(device).thenReturn(true);
              }
              return Mono.just(false);
            })
        .defaultIfEmpty(false);
  }

  private Mono<Boolean> verifyEmailOtp(User user, String code) {
    return otpService
        .verify(user, authProperties.mfaEmailPurpose(), code)
        .thenReturn(true)
        .onErrorResume(AuthenticationFailedException.class, error -> Mono.just(false));
  }

  private Mono<Boolean> verifyRecovery(User user, String code) {
    return recoveryCodeRepository
        .findUsableByHash(user.getId(), RecoveryCodes.hash(code))
        .flatMap(
            recovery ->
                recoveryCodeRepository
                    .consumeIfUnused(recovery.getId(), Instant.now())
                    .map(affected -> affected == CONSUMED))
        .defaultIfEmpty(false);
  }
}
