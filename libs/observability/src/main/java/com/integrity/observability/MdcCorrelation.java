package com.integrity.observability;

import com.integrity.logging.MdcKeys;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

/**
 * Associates a correlation id with the MDC and the Reactor context for the duration of a reactive
 * chain.
 *
 * <p>Used by event consumers and other asynchronous entry points that have no incoming HTTP request
 * to guarantee structured log entries carry a stable request and trace id across threads. Relies on
 * {@link reactor.core.publisher.Hooks#enableAutomaticContextPropagation()} to propagate the values
 * to operator threads.
 */
public final class MdcCorrelation {

  private MdcCorrelation() {}

  /**
   * Wraps the given source so that the correlation id is placed in the MDC and the Reactor context
   * for the lifetime of the subscription, restoring any previous MDC values on completion.
   *
   * @param source the publisher to run under the correlation id
   * @param correlationId the id to publish as {@link MdcKeys#REQUEST_ID} and {@link
   *     MdcKeys#TRACE_ID}
   * @param <T> the publisher value type
   * @return a publisher that runs {@code source} with the correlation id in scope
   */
  public static <T> Mono<T> withCorrelationId(Mono<T> source, String correlationId) {
    return Mono.using(
        () -> new Scope(correlationId),
        scope ->
            source.contextWrite(
                context -> context.put(CorrelationIdWebFilter.CONTEXT_REQUEST_ID, correlationId)),
        Scope::close);
  }

  /** Captures the previous MDC values and installs the correlation id until closed. */
  private static final class Scope implements AutoCloseable {

    private final String previousRequestId;
    private final String previousTraceId;

    Scope(String correlationId) {
      previousRequestId = MDC.get(MdcKeys.REQUEST_ID);
      previousTraceId = MDC.get(MdcKeys.TRACE_ID);
      MDC.put(MdcKeys.REQUEST_ID, correlationId);
      MDC.put(MdcKeys.TRACE_ID, correlationId);
    }

    @Override
    public void close() {
      restore(MdcKeys.REQUEST_ID, previousRequestId);
      restore(MdcKeys.TRACE_ID, previousTraceId);
    }

    private static void restore(String key, String previous) {
      if (previous == null) {
        MDC.remove(key);
      } else {
        MDC.put(key, previous);
      }
    }
  }
}
