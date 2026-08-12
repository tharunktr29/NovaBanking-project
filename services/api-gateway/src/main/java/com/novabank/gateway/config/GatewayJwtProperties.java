package com.novabank.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "novabank.jwt")
public record GatewayJwtProperties(String secret) {
}
