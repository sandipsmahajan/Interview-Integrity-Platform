package com.integrity.desktopclient.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

class SessionRegistryTest {

  private final SessionRegistry registry = new SessionRegistry();

  @Test
  void registerReturnsDedicatedSinkPerSession() {
    Sinks.Many<String> first = registry.register("s-1");
    Sinks.Many<String> second = registry.register("s-2");

    assertThat(first).isNotSameAs(second);
    assertThat(registry.size()).isEqualTo(2);
  }

  @Test
  void broadcastDeliversToEverySession() {
    List<String> received = new CopyOnWriteArrayList<>();
    registry.register("s-1").asFlux().subscribe(received::add);
    registry.register("s-2").asFlux().subscribe(received::add);

    registry.broadcast("hello");

    assertThat(received).containsExactly("hello", "hello");
  }

  @Test
  void unregisterRemovesSessionAndCompletesItsSink() {
    Sinks.Many<String> sink = registry.register("s-1");
    boolean[] completed = {false};
    sink.asFlux().doOnComplete(() -> completed[0] = true).subscribe();

    registry.unregister("s-1");

    assertThat(registry.size()).isZero();
    assertThat(completed[0]).isTrue();
  }
}
