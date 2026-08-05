package com.integrity.api;

import java.util.List;

/**
 * Page of results returned by list endpoints.
 *
 * @param items the current page of results
 * @param page zero based page number
 * @param size requested page size
 * @param totalElements total number of matching results
 * @param totalPages total number of pages
 */
public record PageResponse<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {}
