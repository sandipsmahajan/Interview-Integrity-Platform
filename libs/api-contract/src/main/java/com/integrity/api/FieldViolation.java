package com.integrity.api;

/**
 * A single field level validation violation.
 *
 * @param field name of the offending field, dot separated for nested paths
 * @param message human readable description of the violation
 */
public record FieldViolation(String field, String message) {}
