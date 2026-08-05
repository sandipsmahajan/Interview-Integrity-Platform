package com.integrity.identity.web;

import com.integrity.identity.service.AuthService;
import com.integrity.identity.web.dto.LoginRequest;
import com.integrity.identity.web.dto.LoginResult;
import com.integrity.identity.web.dto.LogoutRequest;
import com.integrity.identity.web.dto.PasswordResetResponse;
import com.integrity.identity.web.dto.RefreshRequest;
import com.integrity.identity.web.dto.RegisterOrganizationRequest;
import com.integrity.identity.web.dto.RequestPasswordResetRequest;
import com.integrity.identity.web.dto.ResetPasswordRequest;
import com.integrity.identity.web.dto.TokenResponse;
import com.integrity.identity.web.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Public authentication endpoints. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, refresh and recover access")
public final class AuthController {

  private final AuthService authService;

  /** Creates the controller bound to the auth service. */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Registers a new organization with its first administrator. */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register organization and first administrator")
  public Mono<TokenResponse> register(@Valid @RequestBody RegisterOrganizationRequest request) {
    return authService.register(request);
  }

  /** Authenticates a user and issues tokens, or returns an MFA challenge. */
  @PostMapping("/login")
  @Operation(summary = "Authenticate and receive tokens or an MFA challenge")
  public Mono<Object> login(
      @Valid @RequestBody LoginRequest request,
      @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
    return authService
        .login(request, resolveIp(forwardedFor))
        .map(
            result ->
                result instanceof LoginResult.Authenticated authenticated
                    ? authenticated.tokens()
                    : ((LoginResult.MfaRequired) result).challenge());
  }

  /** Rotates the refresh token and issues a fresh token pair. */
  @PostMapping("/refresh")
  @Operation(summary = "Rotate refresh token")
  public Mono<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  /** Revokes the session associated with the refresh token. */
  @PostMapping("/logout")
  @Operation(summary = "Terminate session")
  public Mono<Void> logout(@Valid @RequestBody LogoutRequest request) {
    return authService.logout(request);
  }

  /** Verifies a user email with a purpose token. */
  @PostMapping("/verify-email")
  @Operation(summary = "Verify email with one-time token")
  public Mono<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return authService.verifyEmail(request);
  }

  /** Requests a password reset for an email. */
  @PostMapping("/password/reset-request")
  @Operation(summary = "Request password reset")
  public Mono<PasswordResetResponse> requestPasswordReset(
      @Valid @RequestBody RequestPasswordResetRequest request) {
    return authService.requestPasswordReset(request);
  }

  /** Completes a password reset with the one-time token. */
  @PostMapping("/password/reset")
  @Operation(summary = "Complete password reset")
  public Mono<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return authService.resetPassword(request);
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
