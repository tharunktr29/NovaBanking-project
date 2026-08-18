package com.novabank.card.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient accountRestClient(CardProperties properties) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(properties.internalTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.internalTimeoutMs()));
        return RestClient.builder()
                .baseUrl(properties.accountServiceUri())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
