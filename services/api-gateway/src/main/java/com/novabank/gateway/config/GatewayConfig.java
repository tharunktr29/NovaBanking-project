package com.novabank.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewayConfig {
    @Bean
    KeyResolver customerOrIpKeyResolver() {
        return exchange -> {
            var principalName = exchange.getPrincipal().map(principal -> principal.getName()).defaultIfEmpty("");
            return principalName.flatMap(name -> {
                if (!name.isBlank()) {
                    return Mono.just(name);
                }
                var remoteAddress = exchange.getRequest().getRemoteAddress();
                return Mono.just(remoteAddress == null ? "anonymous" : remoteAddress.getAddress().getHostAddress());
            });
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id", "Idempotency-Key"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
