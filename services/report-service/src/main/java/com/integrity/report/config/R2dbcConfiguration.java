package com.integrity.report.config;

import com.integrity.report.domain.ReportType;
import com.integrity.report.domain.ReportStatus;
import com.integrity.report.domain.ReportFormat;
import com.integrity.common.JsonbConverters;
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

@Configuration
public class R2dbcConfiguration {

  @Bean
  ConnectionFactoryOptionsBuilderCustomizer postgresEnumCodecCustomizer() {
    return options ->
        options.option(
            PostgresqlConnectionFactoryProvider.EXTENSIONS,
            List.of(
                EnumCodec.builder()
                    .withEnum("report_type", ReportType.class)
                    .withEnum("report_status", ReportStatus.class)
                    .withEnum("report_format", ReportFormat.class)                    .build()));
  }

  @WritingConverter
  static final class ReportTypeWriteConverter extends EnumWriteSupport<ReportType> {}

  @WritingConverter
  static final class ReportStatusWriteConverter extends EnumWriteSupport<ReportStatus> {}

  @WritingConverter
  static final class ReportFormatWriteConverter extends EnumWriteSupport<ReportFormat> {}

  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes = new SimpleTypeHolder(Set.of(Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new ReportTypeWriteConverter());
    converters.add(new ReportStatusWriteConverter());
    converters.add(new ReportFormatWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
