package com.novabank.payment.service;

import com.novabank.payment.dto.internal.HistoryTransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TransactionHistoryClient {
    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryClient.class);
    private final RestClient restClient;

    public TransactionHistoryClient(@Qualifier("transactionRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void record(HistoryTransactionRequest request, String authHeader, String correlationId) {
        try {
            restClient.post()
                    .uri("/api/internal/transactions/history")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header("X-Correlation-ID", correlationId)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Payment history projection write failed for sourceReference={}", request.sourceReference());
        }
    }
}
