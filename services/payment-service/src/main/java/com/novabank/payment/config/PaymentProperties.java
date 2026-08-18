package com.novabank.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.payments")
public record PaymentProperties(String accountServiceUri, String cardServiceUri, String transactionServiceUri, int internalTimeoutMs) {
}
