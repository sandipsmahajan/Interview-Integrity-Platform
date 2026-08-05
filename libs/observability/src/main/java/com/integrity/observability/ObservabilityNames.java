package com.integrity.observability;

/** Metric and tag names used consistently for Prometheus and tracing. */
public final class ObservabilityNames {
  private ObservabilityNames() {}

  /** Counter of events published to Kafka. */
  public static final String EVENTS_PUBLISHED = "platform.events.published";

  /** Counter of events consumed from Kafka. */
  public static final String EVENTS_CONSUMED = "platform.events.consumed";

  /** Gauge of active database connections in the pool. */
  public static final String DB_ACTIVE_CONNECTIONS = "platform.db.connections.active";

  /** Timer for REST endpoint latency, tagged with {@code uri} and {@code method}. */
  public static final String HTTP_SERVER_REQUESTS = "platform.http.server.requests";

  /** Common tag key for the service name. */
  public static final String TAG_SERVICE = "service";

  /** Common tag key for the tenant id. */
  public static final String TAG_ORGANIZATION = "organizationId";

  /** Common tag key for the HTTP route. */
  public static final String TAG_URI = "uri";
}
