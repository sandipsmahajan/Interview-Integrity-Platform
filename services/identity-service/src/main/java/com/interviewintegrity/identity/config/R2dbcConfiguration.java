package com.interviewintegrity.identity.config;

import com.interviewintegrity.identity.domain.SessionStatus;
import com.interviewintegrity.identity.domain.UserStatus;
import com.interviewintegrity.common.JsonbConverters;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.postgresql.codec.Json;
import java.net.InetAddress;
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
 * <p>PostgreSQL rejects plain {@code varchar} bindings for {@code ENUM} columns, and Spring Data
 * R2DBC by default writes Java enums as their {@code name()} string. This wires two things so enum
 * writes use the enum type OID instead:
 *
 * <ul>
 *   <li>an {@link EnumCodec} registered as a connection extension that encodes/decodes the enum
 *       values of this service using the {@code pg_type} OIDs of {@code user_status} and {@code
 *       session_status};
 *   <li>{@link EnumWriteSupport} converters so Spring Data R2DBC binds the raw Java enums instead
 *       of converting them to strings before they reach the driver.
 * </ul>
 */
@Configuration
public class R2dbcConfiguration {

  /** Registers the PostgreSQL enum codecs for the identity schema enum types. */
  @Bean
  ConnectionFactoryOptionsBuilderCustomizer postgresEnumCodecCustomizer() {
    return options ->
        options.option(
            PostgresqlConnectionFactoryProvider.EXTENSIONS,
            List.of(
                EnumCodec.builder()
                    .withEnum("user_status", UserStatus.class)
                    .withEnum("session_status", SessionStatus.class)
                    .build()));
  }

  /** Keeps Spring Data R2DBC from converting {@link UserStatus} to a plain string on write. */
  @WritingConverter
  static final class UserStatusWriteConverter extends EnumWriteSupport<UserStatus> {}

  /** Keeps Spring Data R2DBC from converting {@link SessionStatus} to a plain string on write. */
  @WritingConverter
  static final class SessionStatusWriteConverter extends EnumWriteSupport<SessionStatus> {}

  /** Extends the default store conversions with the enum write converters and inet support. */
  @Bean
  R2dbcCustomConversions r2dbcCustomConversions() {
    SimpleTypeHolder simpleTypes =
        new SimpleTypeHolder(Set.of(InetAddress.class, Json.class), R2dbcSimpleTypeHolder.HOLDER);
    List<Object> converters = new ArrayList<>(R2dbcCustomConversions.STORE_CONVERTERS);
    converters.add(new UserStatusWriteConverter());
    converters.add(new SessionStatusWriteConverter());
    converters.addAll(JsonbConverters.toList());
    return new R2dbcCustomConversions(
        StoreConversions.of(simpleTypes, R2dbcCustomConversions.STORE_CONVERTERS), converters);
  }
}
