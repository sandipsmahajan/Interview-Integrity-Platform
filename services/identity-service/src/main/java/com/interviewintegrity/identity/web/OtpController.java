package com.interviewintegrity.identity.web;

import com.interviewintegrity.exception.AuthenticationFailedException;
import com.interviewintegrity.identity.domain.User;
import com.interviewintegrity.identity.repository.UserRepository;
import com.interviewintegrity.identity.service.OtpService;
import com.interviewintegrity.identity.web.dto.OtpSendRequest;
import com.interviewintegrity.identity.web.dto.OtpVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Email one-time passcode endpoints used for MFA and verification flows. */
@RestController
@RequestMapping("/api/v1/auth/otp")
@Tag(name = "Email OTP", description = "Request and verify one-time passcodes delivered by email")
public final class OtpController {

  private static final int SINGLE_ACCOUNT = 1;

  private final OtpService otpService;
  private final UserRepository userRepository;

  /** Creates the controller bound to the OTP service and user repository. */
  public OtpController(OtpService otpService, UserRepository userRepository) {
    this.otpService = otpService;
    this.userRepository = userRepository;
  }

  /** Delivers a one-time passcode to the email address of the account. */
  @PostMapping("/send")
  @Operation(summary = "Send an email one-time passcode")
  public Mono<Void> send(@Valid @RequestBody OtpSendRequest request) {
    return resolveSingleUser(request.email())
        .flatMap(user -> otpService.request(user, request.purpose()));
  }

  /** Verifies a one-time passcode, consuming it on success. */
  @PostMapping("/verify")
  @Operation(summary = "Verify an email one-time passcode")
  public Mono<Void> verify(@Valid @RequestBody OtpVerifyRequest request) {
    return resolveSingleUser(request.email())
        .flatMap(user -> otpService.verify(user, request.purpose(), request.code()));
  }

  private Mono<User> resolveSingleUser(String email) {
    String normalized = email.toLowerCase(Locale.ROOT);
    return userRepository
        .findLiveByEmail(normalized)
        .collectList()
        .flatMap(
            users -> {
              if (users.size() != SINGLE_ACCOUNT) {
                return Mono.error(new AuthenticationFailedException("Email not found"));
              }
              return Mono.just(users.get(0));
            });
  }
}
