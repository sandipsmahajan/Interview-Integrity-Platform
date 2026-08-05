package com.integrity.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for opaque refresh token generation and hashing. */
class RefreshTokensTest {

  @Test
  void generatedTokensAreUniqueAndUrlSafe() {
    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      String token = RefreshTokens.generate();
      assertThat(token).doesNotContain("+", "/", "=");
      tokens.add(token);
    }
    assertThat(tokens).hasSize(1000);
  }

  @Test
  void hashIsDeterministicHexDigest() {
    String token = RefreshTokens.generate();

    String first = RefreshTokens.hash(token);
    String second = RefreshTokens.hash(token);

    assertThat(first).isEqualTo(second).hasSize(64);
    assertThat(RefreshTokens.hash("different")).isNotEqualTo(first);
  }

  @Test
  void plaintextIsNeverRecoverableFromHash() {
    String token = RefreshTokens.generate();
    assertThat(RefreshTokens.hash(token)).doesNotContain(token);
  }
}
