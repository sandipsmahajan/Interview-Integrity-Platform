package com.integrity.configuration.config;

import com.integrity.common.JsonbConverters;
import com.integrity.configuration.domain.ConfigScope;
import com.integrity.configuration.domain.ConfigValueType;
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
                    .withEnum("config_scope", ConfigScope.class)
                    .withEnum("config_value_type", ConfigValueType.class)
                    .build()));
  }

  @WritingConverter
  static final class ConfigScopeWriteConverter extends EnumWriteSupport<ConfigScope> {}

  @WritingConverter
  static final class ConfigValueTypeWriteConverter extends EnumWriteSupport<ConfigValueType> {}

  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes =
        new SimpleTypeHolder(Set.of(Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new ConfigScopeWriteConverter());
    converters.add(new ConfigValueTypeWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
