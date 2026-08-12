package com.novabank.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.transactions")
public record TransactionProperties(int exportMaxDays, int exportRowLimit) {
}
