package com.integrity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Unit tests for MFA recovery code generation and hashing. */
class RecoveryCodesTest {

  @Test
  void generateReturnsEightDistinctFormattedCodes() {
    List<String> codes = RecoveryCodes.generate();

    assertThat(codes).hasSize(8);
    assertThat(codes.stream().distinct().count()).isEqualTo(8);
    assertThat(codes).allMatch(code -> code.matches("^\\d{6}-\\d{6}$"));
  }

  @Test
  void generateProducesDifferentSetsAcrossInvocations() {
    List<String> first = RecoveryCodes.generate();
    List<String> second = RecoveryCodes.generate();

    assertThat(first).doesNotContainAnyElementsOf(second);
  }

  @Test
  void hashIsDeterministicAndHidesRawCode() {
    String code = RecoveryCodes.generate().get(0);

    String hash = RecoveryCodes.hash(code);

    assertThat(hash).hasSize(64).matches("^[0-9a-f]+$");
    assertThat(RecoveryCodes.hash(code)).isEqualTo(hash);
    assertThat(hash).isNotEqualTo(code);
  }

  @Test
  void hashDistinguishesDifferentCodes() {
    List<String> codes = RecoveryCodes.generate();

    String hashes = codes.stream().map(RecoveryCodes::hash).collect(Collectors.joining());
    assertThat(codes.stream().map(RecoveryCodes::hash).distinct().count()).isEqualTo(8);
    assertThat(hashes).hasSize(8 * 64);
  }
}
