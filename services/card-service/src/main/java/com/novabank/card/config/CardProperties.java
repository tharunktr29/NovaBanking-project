package com.novabank.card.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.cards")
public record CardProperties(String accountServiceUri, int internalTimeoutMs) {
}
