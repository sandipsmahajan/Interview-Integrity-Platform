package com.integrity.api;

import java.util.List;

/** Factory for {@link PageResponse} instances shared by every service. */
public final class PageResponses {

  private PageResponses() {}

  /**
   * Creates a page of results.
   *
   * @param items the current page of results
   * @param page zero based page number
   * @param size requested page size
   * @param totalElements total number of matching results
   * @param <T> element type
   * @return a populated page response
   */
  public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(items, page, size, totalElements, totalPages);
  }
}
