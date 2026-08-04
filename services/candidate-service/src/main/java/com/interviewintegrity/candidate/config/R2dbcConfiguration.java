package com.interviewintegrity.candidate.config;

import com.interviewintegrity.candidate.domain.AssessmentStatus;
import com.interviewintegrity.candidate.domain.CandidateStatus;
import com.interviewintegrity.candidate.domain.ConsentStatus;
import com.interviewintegrity.common.JsonbConverters;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.postgresql.codec.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.boot.r2dbc.autoconfigure.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.r2dbc.convert.EnumWriteSupport;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.mapping.R2dbcSimpleTypeHolder;

/**
 * Configures R2DBC support for PostgreSQL enum columns.
 *
 * <p>PostgreSQL rejects plain {@code varchar} bindings for {@code ENUM} columns,
 * so we register {@link EnumCodec} extensions and {@link EnumWriteSupport}
 * converters so writes use the enum type OID instead of plain strings.
 */
@Configuration
public class R2dbcConfiguration {

  @Bean
  ConnectionFactoryOptionsBuilderCustomizer postgresEnumCodecCustomizer() {
    return options ->
        options.option(
            PostgresqlConnectionFactoryProvider.EXTENSIONS,
            List.of(
                EnumCodec.builder()
                    .withEnum("candidate_status", CandidateStatus.class)
                    .withEnum("assessment_status", AssessmentStatus.class)
                    .withEnum("consent_status", ConsentStatus.class)
                    .build()));
  }

  @WritingConverter
  static final class CandidateStatusWriteConverter extends EnumWriteSupport<CandidateStatus> {}

  @WritingConverter
  static final class AssessmentStatusWriteConverter extends EnumWriteSupport<AssessmentStatus> {}

  @WritingConverter
  static final class ConsentStatusWriteConverter extends EnumWriteSupport<ConsentStatus> {}

  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes = new SimpleTypeHolder(Set.of(Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new CandidateStatusWriteConverter());
    converters.add(new AssessmentStatusWriteConverter());
    converters.add(new ConsentStatusWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
