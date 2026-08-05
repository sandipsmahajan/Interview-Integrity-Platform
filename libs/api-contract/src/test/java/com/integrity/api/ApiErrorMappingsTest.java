package com.integrity.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.integrity.exception.NotFoundException;
import com.integrity.exception.ValidationFailedException;
import com.integrity.exception.Violation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

/** Unit tests for {@link ApiErrorMappings}. */
class ApiErrorMappingsTest {

  @Test
  void statusMapsDomainExceptions() {
    assertThat(ApiErrorMappings.status(new NotFoundException("missing")))
        .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    assertThat(ApiErrorMappings.status(new ValidationFailedException("bad input")))
        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    assertThat(ApiErrorMappings.status(new IllegalStateException("boom")))
        .isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void codeSurfacesDomainCode() {
    assertThat(ApiErrorMappings.code(new NotFoundException("missing")))
        .isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(ApiErrorMappings.code(new ValidationFailedException("bad input")))
        .isEqualTo("VALIDATION_FAILED");
    assertThat(ApiErrorMappings.code(new IllegalStateException("boom")))
        .isEqualTo("INTERNAL_ERROR");
  }

  @Test
  void violationsCarriedByValidationException() {
    List<FieldViolation> violations =
        ApiErrorMappings.violations(
            new ValidationFailedException(
                "invalid input", List.of(new Violation("email", "must not be blank"))));

    assertThat(violations).containsExactly(new FieldViolation("email", "must not be blank"));
  }

  @Test
  void violationsExtractedFromBindException() {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(
        new FieldError(
            "request",
            "name",
            "must not be null",
            false,
            new String[] {"NotNull"},
            new Object[] {},
            "must not be null"));
    WebExchangeBindException bindException = new WebExchangeBindException(null, bindingResult);

    List<FieldViolation> violations = ApiErrorMappings.violations(bindException);

    assertThat(violations).containsExactly(new FieldViolation("name", "must not be null"));
  }

  @Test
  void violationsEmptyForUnrelatedExceptions() {
    assertThat(ApiErrorMappings.violations(new NotFoundException("missing"))).isEmpty();
    assertThat(ApiErrorMappings.violations(new IllegalStateException("boom"))).isEmpty();
  }
}
