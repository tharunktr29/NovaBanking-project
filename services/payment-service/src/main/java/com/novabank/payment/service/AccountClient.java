package com.novabank.payment.service;

import com.novabank.payment.dto.internal.*;
import com.novabank.payment.exception.PaymentException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class AccountClient {
    private final RestClient restClient;

    public AccountClient(@Qualifier("accountRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AccountDto getAccount(UUID accountId, String authHeader) {
        try {
            return restClient.get()
                    .uri("/api/accounts/{id}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(AccountDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        } catch (HttpClientErrorException ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "ACCOUNT_VALIDATION_FAILED", "Unable to validate account");
        }
    }

    public LedgerTransactionDto postLedger(PostLedgerTransactionRequest request, String authHeader, String correlationId) {
        try {
            return restClient.post()
                    .uri("/api/internal/ledger/transactions")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header("X-Correlation-ID", correlationId)
                    .body(request)
                    .retrieve()
                    .body(LedgerTransactionDto.class);
        } catch (HttpClientErrorException.Conflict ex) {
            throw new PaymentException(HttpStatus.CONFLICT, "LEDGER_CONFLICT", "The ledger rejected the money movement request");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "LEDGER_REJECTED", "The source account has insufficient available funds or is not eligible");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
    }
}
