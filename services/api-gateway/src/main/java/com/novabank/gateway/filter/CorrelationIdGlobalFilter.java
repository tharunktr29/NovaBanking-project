package com.novabank.gateway.filter;

import com.novabank.shared.correlation.Correlation;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var incomingCorrelationId = exchange.getRequest().getHeaders().getFirst(Correlation.HEADER_NAME);
        var correlationId = StringUtils.hasText(incomingCorrelationId)
                ? incomingCorrelationId
                : UUID.randomUUID().toString();
        var requestHeaders = new HttpHeaders();
        requestHeaders.putAll(exchange.getRequest().getHeaders());
        requestHeaders.set(Correlation.HEADER_NAME, correlationId);
        var mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return requestHeaders;
            }
        };
        var mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        mutatedExchange.getResponse().getHeaders().set(Correlation.HEADER_NAME, correlationId);
        MDC.put(Correlation.MDC_KEY, correlationId);
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(Correlation.MDC_KEY));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
