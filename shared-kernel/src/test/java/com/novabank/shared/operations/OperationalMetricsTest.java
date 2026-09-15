package com.novabank.shared.operations;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OperationalMetricsTest {
 @Test void usesOnlyOperationAndOutcomeLabels(){var registry=new SimpleMeterRegistry();var metrics=new OperationalMetrics(registry);metrics.success("statement.generate");var id=registry.get("novabank.business.operations").counter().getId();assertEquals(2,id.getTags().size());assertTrue(id.getTags().stream().noneMatch(t->t.getKey().matches("customer|account|payment|email")));}
}
