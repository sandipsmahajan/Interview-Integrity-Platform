package com.interviewintegrity.identity.service;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 time-based one-time password support for TOTP MFA devices.
 *
 * <p>Implements the widely deployed TOTP profile: HMAC-SHA1 with a 30 second time step and six
 * digit codes, base32 encoded secrets as produced by authenticator applications.
 */
public final class Totp {

  private static final int TIME_STEP_SECONDS = 30;
  private static final int CODE_DIGITS = 6;
  private static final int SECRET_BYTES = 20;
  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

  private Totp() {}

  /** Generates a new random base32 TOTP secret suitable for enrollment. */
  public static String generateSecret() {
    byte[] bytes = new byte[SECRET_BYTES];
    RANDOM.nextBytes(bytes);
    return Base32.encode(bytes);
  }

  /**
   * Validates a six digit code against the secret within the given clock skew window.
   *
   * @param base32Secret the base32 encoded TOTP secret
   * @param code the six digit code supplied by the user
   * @param window number of time steps of tolerance either side of the current step
   * @return true when the code matches a step in the window
   */
  public static boolean verify(String base32Secret, String code, int window) {
    if (base32Secret == null || code == null || code.length() != CODE_DIGITS) {
      return false;
    }
    if (!code.chars().allMatch(Character::isDigit)) {
      return false;
    }
    try {
      byte[] key = Base32.decode(base32Secret);
      long counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
      for (long offset = -window; offset <= window; offset++) {
        if (constantTimeEquals(generateCode(key, counter + offset), code)) {
          return true;
        }
      }
      return false;
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      return false;
    }
  }

  /** Builds the otpauth:// provisioning URI for authenticator applications. */
  public static String otpauthUri(String issuer, String accountName, String base32Secret) {
    String label = percentEncode(issuer) + ":" + percentEncode(accountName);
    return "otpauth://totp/"
        + label
        + "?secret="
        + base32Secret
        + "&issuer="
        + percentEncode(issuer)
        + "&algorithm=SHA1&digits=6&period=30";
  }

  private static String generateCode(byte[] key, long counter) throws GeneralSecurityException {
    Mac mac = Mac.getInstance(HMAC_ALGORITHM);
    mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
    byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
    int offset = hash[hash.length - 1] & 0x0f;
    int binary =
        ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);
    int modulus = 1;
    for (int i = 0; i < CODE_DIGITS; i++) {
      modulus *= 10;
    }
    return String.format("%0" + CODE_DIGITS + "d", binary % modulus);
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
  }

  private static String percentEncode(String value) {
    return value.replace(":", "%3A").replace(" ", "%20").replace("?", "%3F").replace("&", "%26");
  }

  /** Minimal RFC 4648 base32 codec for TOTP secrets. */
  private static final class Base32 {

    private static final int GROUP_BITS = 5;
    private static final int BYTE_BITS = 8;

    private Base32() {}

    static byte[] decode(String input) {
      String normalized = input.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
      int outputLength = normalized.length() * GROUP_BITS / BYTE_BITS;
      byte[] output = new byte[outputLength];
      int buffer = 0;
      int bitsLeft = 0;
      int index = 0;
      for (int i = 0; i < normalized.length(); i++) {
        int value = decodeChar(normalized.charAt(i));
        buffer = (buffer << GROUP_BITS) | value;
        bitsLeft += GROUP_BITS;
        if (bitsLeft >= BYTE_BITS) {
          output[index++] = (byte) ((buffer >> (bitsLeft - BYTE_BITS)) & 0xff);
          bitsLeft -= BYTE_BITS;
        }
      }
      return output;
    }

    private static int decodeChar(char c) {
      return switch (c) {
        case 'A',
                'B',
                'C',
                'D',
                'E',
                'F',
                'G',
                'H',
                'I',
                'J',
                'K',
                'L',
                'M',
                'N',
                'O',
                'P',
                'Q',
                'R',
                'S',
                'T',
                'U',
                'V',
                'W',
                'X',
                'Y',
                'Z' ->
            c - 'A';
        case '2', '3', '4', '5', '6', '7' -> c - '2' + 26;
        default -> throw new IllegalArgumentException("Invalid base32 character");
      };
    }

    static String encode(byte[] data) {
      StringBuilder builder = new StringBuilder();
      int buffer = 0;
      int bitsLeft = 0;
      for (byte b : data) {
        buffer = (buffer << BYTE_BITS) | (b & 0xff);
        bitsLeft += BYTE_BITS;
        while (bitsLeft >= GROUP_BITS) {
          builder.append(BASE32_ALPHABET[(buffer >> (bitsLeft - GROUP_BITS)) & 0x1f]);
          bitsLeft -= GROUP_BITS;
        }
      }
      if (bitsLeft > 0) {
        builder.append(BASE32_ALPHABET[(buffer << (GROUP_BITS - bitsLeft)) & 0x1f]);
      }
      return builder.toString();
    }
  }
}
