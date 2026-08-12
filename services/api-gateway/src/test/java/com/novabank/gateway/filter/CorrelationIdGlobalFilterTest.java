package com.novabank.gateway.filter;

import com.novabank.shared.correlation.Correlation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {
    @Test
    void propagatesExistingCorrelationId() {
        var filter = new CorrelationIdGlobalFilter();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login")
                .header(Correlation.HEADER_NAME, "corr-123"));

        StepVerifier.create(filter.filter(exchange, chain -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(Correlation.HEADER_NAME)).isEqualTo("corr-123");
    }
}
