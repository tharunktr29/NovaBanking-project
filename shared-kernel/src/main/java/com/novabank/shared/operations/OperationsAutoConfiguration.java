package com.novabank.shared.operations;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class OperationsAutoConfiguration {
    @Bean @ConditionalOnMissingBean OperationalMetrics operationalMetrics(MeterRegistry registry) { return new OperationalMetrics(registry); }
}
