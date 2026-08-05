package com.integrity.exception;

import java.io.Serializable;

/**
 * A single field level validation violation carried by {@link ValidationFailedException}.
 *
 * @param field name of the offending field, dot separated for nested paths
 * @param message human readable description of the violation
 */
public record Violation(String field, String message) implements Serializable {}
