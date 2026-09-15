package com.novabank.shared.operations;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Callable;

/** Low-cardinality operational metrics. Never pass customer or financial identifiers as tags. */
public final class OperationalMetrics {
    private final MeterRegistry registry;
    public OperationalMetrics(MeterRegistry registry) { this.registry = registry; }
    public void success(String operation) { counter(operation, "success").increment(); }
    public void failure(String operation) { counter(operation, "failure").increment(); }
    public void duplicateEvent() { registry.counter("novabank.kafka.duplicate.events").increment(); }
    public void deadLetter() { registry.counter("novabank.kafka.dead.letter.events").increment(); }
    public <T> T timed(String operation, Callable<T> work) {
        try { return Timer.builder("novabank.operation.duration").tag("operation", safe(operation)).register(registry).recordCallable(work); }
        catch (RuntimeException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private Counter counter(String operation, String outcome) {
        return Counter.builder("novabank.business.operations").tag("operation", safe(operation)).tag("outcome", outcome).register(registry);
    }
    private String safe(String value) { return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9_.-]", "_"); }
}
