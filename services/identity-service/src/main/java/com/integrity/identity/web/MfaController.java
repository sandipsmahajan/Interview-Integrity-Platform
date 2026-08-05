package com.integrity.identity.web;

import com.integrity.exception.NotFoundException;
import com.integrity.identity.domain.User;
import com.integrity.identity.repository.UserRepository;
import com.integrity.identity.service.MfaService;
import com.integrity.identity.web.dto.MfaDeviceResponse;
import com.integrity.identity.web.dto.MfaEmailOtpRequest;
import com.integrity.identity.web.dto.MfaEnrollResponse;
import com.integrity.identity.web.dto.MfaTotpVerifyRequest;
import com.integrity.identity.web.dto.MfaVerifyRequest;
import com.integrity.identity.web.dto.RecoveryCodesResponse;
import com.integrity.identity.web.dto.TokenResponse;
import com.integrity.identity.web.dto.TrustedDeviceResponse;
import com.integrity.security.SecurityPrincipals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Multi-factor authentication endpoints: TOTP enrollment, challenges and trusted devices. */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@Tag(name = "Multi-factor authentication", description = "Enroll, verify and manage MFA devices")
public final class MfaController {

  private final MfaService mfaService;
  private final UserRepository userRepository;

  /** Creates the controller bound to the MFA service and user repository. */
  public MfaController(MfaService mfaService, UserRepository userRepository) {
    this.mfaService = mfaService;
    this.userRepository = userRepository;
  }

  /** Starts a TOTP enrollment and returns the secret and recovery codes. */
  @PostMapping("/totp/enroll")
  @Operation(summary = "Start TOTP enrollment")
  public Mono<MfaEnrollResponse> enroll(Authentication authentication) {
    return currentUser(authentication).flatMap(mfaService::enrollTotp);
  }

  /** Activates the pending TOTP device with a code from the authenticator. */
  @PostMapping("/totp/verify")
  @Operation(summary = "Activate a pending TOTP device")
  public Mono<Void> verifyEnrollment(
      @Valid @RequestBody MfaTotpVerifyRequest request, Authentication authentication) {
    return currentUser(authentication)
        .flatMap(user -> mfaService.verifyTotpEnrollment(user, request.code()));
  }

  /** Lists the MFA devices of the authenticated user. */
  @GetMapping("/devices")
  @Operation(summary = "List MFA devices")
  public Mono<List<MfaDeviceResponse>> devices(Authentication authentication) {
    return mfaService.listDevices(SecurityPrincipals.userId(authentication));
  }

  /** Removes an MFA device of the authenticated user. */
  @DeleteMapping("/devices/{deviceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove an MFA device")
  public Mono<Void> removeDevice(@PathVariable UUID deviceId, Authentication authentication) {
    return mfaService.removeDevice(SecurityPrincipals.userId(authentication), deviceId);
  }

  /** Regenerates the recovery code set, invalidating all previous codes. */
  @PostMapping("/recovery-codes/regenerate")
  @Operation(summary = "Regenerate recovery codes")
  public Mono<RecoveryCodesResponse> regenerate(Authentication authentication) {
    return currentUser(authentication)
        .flatMap(mfaService::regenerateRecoveryCodes)
        .map(RecoveryCodesResponse::new);
  }

  /** Lists the devices trusted to skip MFA challenges. */
  @GetMapping("/trusted-devices")
  @Operation(summary = "List trusted devices")
  public Mono<List<TrustedDeviceResponse>> trustedDevices(Authentication authentication) {
    return mfaService.listTrustedDevices(SecurityPrincipals.userId(authentication));
  }

  /** Stops trusting a device, requiring MFA challenges on it again. */
  @DeleteMapping("/trusted-devices/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove a trusted device")
  public Mono<Void> removeTrustedDevice(@PathVariable UUID id, Authentication authentication) {
    return mfaService.removeTrustedDevice(SecurityPrincipals.userId(authentication), id);
  }

  /** Completes an MFA challenge and returns tokens, optionally trusting the device. */
  @PostMapping("/verify")
  @Operation(summary = "Complete an MFA login challenge")
  public Mono<TokenResponse> verify(
      @Valid @RequestBody MfaVerifyRequest request,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
    return mfaService.verifyChallenge(
        request.challengeId(),
        request.code(),
        request.trustDevice(),
        request.deviceId(),
        request.deviceName(),
        resolveIp(forwardedFor),
        null);
  }

  /** Sends an email OTP for the user behind a pending MFA challenge. */
  @PostMapping("/email-otp")
  @Operation(summary = "Send an email OTP for an MFA challenge")
  public Mono<Void> emailOtp(@Valid @RequestBody MfaEmailOtpRequest request) {
    return mfaService.sendEmailOtp(request.challengeId());
  }

  private Mono<User> currentUser(Authentication authentication) {
    UUID userId = SecurityPrincipals.userId(authentication);
    return userRepository
        .findLiveById(userId)
        .switchIfEmpty(Mono.error(new NotFoundException("User no longer exists")));
  }

  private String resolveIp(String forwardedFor) {
    if (forwardedFor == null || forwardedFor.isBlank()) {
      return null;
    }
    int comma = forwardedFor.indexOf(',');
    String first = comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor;
    return first.trim();
  }
}
