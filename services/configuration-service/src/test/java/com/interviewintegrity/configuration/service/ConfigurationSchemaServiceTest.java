package com.interviewintegrity.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interviewintegrity.configuration.domain.ConfigValueType;
import com.interviewintegrity.configuration.domain.ConfigurationSchema;
import com.interviewintegrity.configuration.repository.ConfigurationSchemaRepository;
import com.interviewintegrity.exception.ConflictException;
import com.interviewintegrity.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for the configuration schema service. */
@ExtendWith(MockitoExtension.class)
class ConfigurationSchemaServiceTest {

  @Mock private ConfigurationSchemaRepository schemaRepository;

  private ConfigurationSchemaService schemaService;

  @BeforeEach
  void setUp() {
    schemaService = new ConfigurationSchemaService(schemaRepository);
  }

  @Test
  void createDeclaresNewSchemaEntry() {
    when(schemaRepository.existsByKey("audit.retention.days")).thenReturn(Mono.just(false));
    when(schemaRepository.save(any(ConfigurationSchema.class)))
        .thenAnswer(
            invocation -> {
              ConfigurationSchema schema = invocation.getArgument(0);
              schema.setId(UUID.randomUUID());
              return Mono.just(schema);
            });

    StepVerifier.create(
            schemaService.create(
                "audit.retention.days", ConfigValueType.NUMBER, "90", "{}", "Audit retention"))
        .assertNext(
            schema -> {
              assertThat(schema.getKey()).isEqualTo("audit.retention.days");
              assertThat(schema.getValueType()).isEqualTo(ConfigValueType.NUMBER);
              assertThat(schema.getConstraints()).isEqualTo("{}");
            })
        .verifyComplete();
  }

  @Test
  void createRejectsDuplicateKey() {
    when(schemaRepository.existsByKey("audit.retention.days")).thenReturn(Mono.just(true));

    StepVerifier.create(
            schemaService.create(
                "audit.retention.days", ConfigValueType.NUMBER, "90", "{}", "Audit retention"))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  void getReturnsSchemaEntry() {
    UUID schemaId = UUID.randomUUID();
    ConfigurationSchema schema =
        new ConfigurationSchema(
            "audit.retention.days", ConfigValueType.NUMBER, "90", "{}", "Audit retention");
    schema.setId(schemaId);

    when(schemaRepository.findById(schemaId)).thenReturn(Mono.just(schema));

    StepVerifier.create(schemaService.get(schemaId))
        .assertNext(result -> assertThat(result.getKey()).isEqualTo("audit.retention.days"))
        .verifyComplete();
  }

  @Test
  void getReturnsNotFoundForUnknownId() {
    UUID schemaId = UUID.randomUUID();
    when(schemaRepository.findById(schemaId)).thenReturn(Mono.empty());

    StepVerifier.create(schemaService.get(schemaId)).expectError(NotFoundException.class).verify();
  }

  @Test
  void updateReplacesDeclaration() {
    UUID schemaId = UUID.randomUUID();
    ConfigurationSchema schema =
        new ConfigurationSchema(
            "audit.retention.days", ConfigValueType.NUMBER, "90", "{}", "Audit retention");
    schema.setId(schemaId);

    when(schemaRepository.findById(schemaId)).thenReturn(Mono.just(schema));
    when(schemaRepository.save(any(ConfigurationSchema.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(
            schemaService.update(schemaId, ConfigValueType.STRING, "60", "{\"min\":1}", "Updated"))
        .assertNext(
            result -> {
              assertThat(result.getValueType()).isEqualTo(ConfigValueType.STRING);
              assertThat(result.getDefaultValue()).isEqualTo("60");
              assertThat(result.getDescription()).isEqualTo("Updated");
            })
        .verifyComplete();
  }
}
