package com.integrity.logging;

/** MDC key names used for structured logging across all services. */
public final class MdcKeys {
  private MdcKeys() {}

  /** Correlation id propagated through the request chain. */
  public static final String REQUEST_ID = "requestId";

  /** Authenticated user id. */
  public static final String USER_ID = "userId";

  /** Tenant (organization) id. */
  public static final String ORGANIZATION_ID = "organizationId";

  /** Trace id injected by the tracing bridge. */
  public static final String TRACE_ID = "traceId";

  /** Name of the publishing service. */
  public static final String SERVICE = "service";
}
