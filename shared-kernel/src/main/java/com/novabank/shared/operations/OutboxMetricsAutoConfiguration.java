package com.novabank.shared.operations;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
@AutoConfiguration
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass(JdbcTemplate.class)
public class OutboxMetricsAutoConfiguration {
 @Bean @ConditionalOnBean(JdbcTemplate.class) @ConditionalOnMissingBean OutboxMetricsBinder outboxMetricsBinder(JdbcTemplate jdbc, MeterRegistry registry){var binder=new OutboxMetricsBinder(jdbc);binder.bindTo(registry);return binder;}
}
