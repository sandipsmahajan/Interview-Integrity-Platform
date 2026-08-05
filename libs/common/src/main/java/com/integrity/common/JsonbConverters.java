package com.integrity.common;

import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
final class JsonbReadingConverter
    implements org.springframework.core.convert.converter.Converter<Json, String> {

  @Override
  public String convert(Json source) {
    return source.asString();
  }
}

public final class JsonbConverters {

  public static final JsonbReadingConverter READING = new JsonbReadingConverter();

  public static List<Object> toList() {
    return List.of(READING);
  }

  private JsonbConverters() {}
}
