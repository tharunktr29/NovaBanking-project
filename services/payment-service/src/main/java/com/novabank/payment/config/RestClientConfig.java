package com.novabank.payment.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient accountRestClient(PaymentProperties properties) {
        return client(properties.accountServiceUri(), properties.internalTimeoutMs());
    }

    @Bean
    RestClient cardRestClient(PaymentProperties properties) {
        return client(properties.cardServiceUri(), properties.internalTimeoutMs());
    }

    @Bean
    RestClient transactionRestClient(PaymentProperties properties) {
        return client(properties.transactionServiceUri(), properties.internalTimeoutMs());
    }

    private RestClient client(String baseUrl, int timeoutMs) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(ClientHttpRequestFactories.get(settings)).build();
    }
}
