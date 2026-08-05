package com.integrity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** Unit tests for RFC 6238 TOTP generation and verification. */
class TotpTest {

  private static final int GROUP_BITS = 5;
  private static final int BYTE_BITS = 8;

  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final int TIME_STEP_SECONDS = 30;

  @Test
  void generateSecretProducesBase32ValueOfExpectedLength() {
    String secret = Totp.generateSecret();

    assertThat(secret).hasSize(32);
    assertThat(secret).matches("^[A-Z2-7]+$");
    assertThat(secret).doesNotContain("1", "0", "8", "9");
  }

  @Test
  void verifyAcceptsCurrentStepCode() {
    String secret = Totp.generateSecret();

    assertThat(Totp.verify(secret, referenceCode(secret, 0), 1)).isTrue();
  }

  @Test
  void verifyAcceptsCodeWithinSkewWindow() {
    String secret = Totp.generateSecret();

    assertThat(Totp.verify(secret, referenceCode(secret, -1), 1)).isTrue();
    assertThat(Totp.verify(secret, referenceCode(secret, 1), 1)).isTrue();
    assertThat(Totp.verify(secret, referenceCode(secret, -2), 1)).isFalse();
  }

  @Test
  void verifyRejectsWrongCode() {
    String secret = Totp.generateSecret();
    String valid = referenceCode(secret, 0);
    String wrong = "000000".equals(valid) ? "000001" : "000000";

    assertThat(Totp.verify(secret, wrong, 1)).isFalse();
  }

  @Test
  void verifyRejectsMalformedInput() {
    String secret = Totp.generateSecret();

    assertThat(Totp.verify(null, "123456", 1)).isFalse();
    assertThat(Totp.verify(secret, null, 1)).isFalse();
    assertThat(Totp.verify(secret, "12345", 1)).isFalse();
    assertThat(Totp.verify(secret, "abcdef", 1)).isFalse();
    assertThat(Totp.verify("not-base32!", "123456", 1)).isFalse();
  }

  @Test
  void otpauthUriCarriesIssuerAndSecret() {
    String uri = Totp.otpauthUri("Integrity Pro", "alice@example.com", "SECRETSECRETSECRETSECRET");

    assertThat(uri).startsWith("otpauth://totp/");
    assertThat(uri).contains("issuer=Integrity%20Pro");
    assertThat(uri).contains("secret=SECRETSECRETSECRETSECRET");
    assertThat(uri).contains("digits=6&period=30");
  }

  /** Independent RFC 6238 reference implementation used to validate {@link Totp}. */
  private static String referenceCode(String base32Secret, int counterOffset) {
    try {
      byte[] key = rfc4648Base32Decode(base32Secret);
      long counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS + counterOffset;
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
      byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
      int offset = hash[hash.length - 1] & 0x0f;
      int binary =
          ((hash[offset] & 0x7f) << 24)
              | ((hash[offset + 1] & 0xff) << 16)
              | ((hash[offset + 2] & 0xff) << 8)
              | (hash[offset + 3] & 0xff);
      return String.format("%06d", binary % 1_000_000);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC unavailable", e);
    }
  }

  private static byte[] rfc4648Base32Decode(String input) {
    String normalized = input.toUpperCase(Locale.ROOT).replace("=", "");
    int outputLength = normalized.length() * GROUP_BITS / BYTE_BITS;
    byte[] output = new byte[outputLength];
    int buffer = 0;
    int bitsLeft = 0;
    int index = 0;
    for (int i = 0; i < normalized.length(); i++) {
      int value = base32CharValue(normalized.charAt(i));
      buffer = (buffer << GROUP_BITS) | value;
      bitsLeft += GROUP_BITS;
      if (bitsLeft >= BYTE_BITS) {
        output[index++] = (byte) ((buffer >> (bitsLeft - BYTE_BITS)) & 0xff);
        bitsLeft -= BYTE_BITS;
      }
    }
    return output;
  }

  private static int base32CharValue(char c) {
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
      default -> throw new IllegalArgumentException("Invalid base32 character: " + c);
    };
  }
}
