package com.novabank.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.jwt")
public record JwtProperties(String issuer, String secret) {
}
