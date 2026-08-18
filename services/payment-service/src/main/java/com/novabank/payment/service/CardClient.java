package com.novabank.payment.service;

import com.novabank.payment.dto.internal.ApplyCreditCardPaymentRequest;
import com.novabank.payment.dto.internal.ApplyCreditCardPaymentResponse;
import com.novabank.payment.dto.internal.CardDto;
import com.novabank.payment.exception.PaymentException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class CardClient {
    private final RestClient restClient;

    public CardClient(@Qualifier("cardRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public CardDto getCard(UUID cardId, String authHeader) {
        try {
            return restClient.get()
                    .uri("/api/cards/{id}", cardId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(CardDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        } catch (HttpClientErrorException ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CARD_VALIDATION_FAILED", "Unable to validate card");
        }
    }

    public ApplyCreditCardPaymentResponse applyPayment(ApplyCreditCardPaymentRequest request, String authHeader, String correlationId) {
        try {
            return restClient.post()
                    .uri("/api/internal/cards/credit-payments")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header("X-Correlation-ID", correlationId)
                    .body(request)
                    .retrieve()
                    .body(ApplyCreditCardPaymentResponse.class);
        } catch (HttpClientErrorException.Conflict ex) {
            throw new PaymentException(HttpStatus.CONFLICT, "CARD_PAYMENT_CONFLICT", "The card payment was already applied differently");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "CARD_PAYMENT_REJECTED", "The card payment was rejected");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found");
        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
    }
}
