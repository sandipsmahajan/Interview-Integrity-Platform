package com.interviewintegrity.integration.config;

import com.interviewintegrity.integration.domain.IntegrationStatus;
import com.interviewintegrity.integration.domain.SyncDirection;
import com.interviewintegrity.integration.domain.SyncStatus;
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

@Configuration
public class R2dbcConfiguration {

  @Bean
  ConnectionFactoryOptionsBuilderCustomizer postgresEnumCodecCustomizer() {
    return options ->
        options.option(
            PostgresqlConnectionFactoryProvider.EXTENSIONS,
            List.of(
                EnumCodec.builder()
                    .withEnum("integration_status", IntegrationStatus.class)
                    .withEnum("sync_direction", SyncDirection.class)
                    .withEnum("sync_status", SyncStatus.class)                    .build()));
  }

  @WritingConverter
  static final class IntegrationStatusWriteConverter extends EnumWriteSupport<IntegrationStatus> {}

  @WritingConverter
  static final class SyncDirectionWriteConverter extends EnumWriteSupport<SyncDirection> {}

  @WritingConverter
  static final class SyncStatusWriteConverter extends EnumWriteSupport<SyncStatus> {}

  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes = new SimpleTypeHolder(Set.of(Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new IntegrationStatusWriteConverter());
    converters.add(new SyncDirectionWriteConverter());
    converters.add(new SyncStatusWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
