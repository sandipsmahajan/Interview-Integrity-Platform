package com.interviewintegrity.configuration.domain;

/** Visibility scope of a configuration entry. */
public enum ConfigScope {
  /** Platform owned, visible to every tenant. */
  SYSTEM,
  /** Scoped to a single tenant. */
  ORGANIZATION,
  /** Scoped to a tenant and a service. */
  SERVICE
}
