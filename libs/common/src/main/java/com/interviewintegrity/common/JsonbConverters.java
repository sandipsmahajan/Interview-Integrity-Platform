package com.interviewintegrity.common;

import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

@ReadingConverter
final class JsonbReadingConverter implements org.springframework.core.convert.converter.Converter<Json, String> {

  @Override
  public String convert(Json source) {
    return source.asString();
  }
}

@WritingConverter
final class JsonbWritingConverter implements org.springframework.core.convert.converter.Converter<String, Json> {

  @Override
  public Json convert(String source) {
    return Json.of(source);
  }
}

public final class JsonbConverters {

  public static final JsonbReadingConverter READING = new JsonbReadingConverter();
  public static final JsonbWritingConverter WRITING = new JsonbWritingConverter();

  public static List<Object> toList() {
    return List.of(READING, WRITING);
  }

  private JsonbConverters() {}
}
