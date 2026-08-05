package com.integrity.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque refresh token generation and hashing.
 *
 * <p>Refresh tokens are high entropy random values handed to the client once. Only their SHA-256
 * hash is persisted so a database leak does not expose usable refresh tokens.
 */
public final class RefreshTokens {

  private static final int TOKEN_BYTES = 48;
  private static final SecureRandom RANDOM = new SecureRandom();

  private RefreshTokens() {}

  /** Generates a new cryptographically random refresh token. */
  public static String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Returns the SHA-256 hex digest of the given raw refresh token. */
  public static String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
