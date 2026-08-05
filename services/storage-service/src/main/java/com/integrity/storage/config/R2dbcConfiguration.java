package com.integrity.storage.config;

import com.integrity.common.JsonbConverters;
import com.integrity.storage.domain.StorageClass;
import com.integrity.storage.domain.UrlPurpose;
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
                    .withEnum("storage_class", StorageClass.class)
                    .withEnum("url_purpose", UrlPurpose.class)
                    .build()));
  }

  @WritingConverter
  static final class StorageClassWriteConverter extends EnumWriteSupport<StorageClass> {}

  @WritingConverter
  static final class UrlPurposeWriteConverter extends EnumWriteSupport<UrlPurpose> {}

  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes =
        new SimpleTypeHolder(Set.of(Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new StorageClassWriteConverter());
    converters.add(new UrlPurposeWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
