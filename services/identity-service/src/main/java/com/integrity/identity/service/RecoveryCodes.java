package com.integrity.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

/**
 * MFA recovery code generation and hashing.
 *
 * <p>Recovery codes are high entropy random values shown to the user once. Only their SHA-256 hash
 * is persisted so a database leak does not expose usable recovery codes.
 */
public final class RecoveryCodes {

  private static final int CODE_COUNT = 8;
  private static final int CODE_BYTES = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private RecoveryCodes() {}

  /** Generates a fresh set of single-use recovery codes. */
  public static List<String> generate() {
    return IntStream.range(0, CODE_COUNT).mapToObj(i -> toCode(randomBytes())).toList();
  }

  private static byte[] randomBytes() {
    byte[] bytes = new byte[CODE_BYTES];
    RANDOM.nextBytes(bytes);
    return bytes;
  }

  /** Returns the SHA-256 hex digest of the given raw recovery code. */
  public static String hash(String rawCode) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawCode.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private static String toCode(byte[] bytes) {
    int first = ((bytes[0] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | (bytes[2] & 0xff);
    int second = ((bytes[3] & 0xff) << 16) | ((bytes[4] & 0xff) << 8) | (bytes[5] & 0xff);
    return String.format("%06d-%06d", first % 1000000, second % 1000000);
  }
}
