package com.novabank.payment.controller;

import com.novabank.payment.dto.PageResponse;
import com.novabank.payment.dto.request.CreditCardPaymentRequest;
import com.novabank.payment.dto.request.ExternalPaymentRequest;
import com.novabank.payment.dto.request.InternalTransferRequest;
import com.novabank.payment.dto.response.PaymentHistoryResponse;
import com.novabank.payment.dto.response.PaymentResponse;
import com.novabank.payment.service.IdempotencyService;
import com.novabank.payment.service.PaymentQueryParser;
import com.novabank.payment.service.PaymentService;
import com.novabank.shared.correlation.Correlation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentQueryParser queryParser;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, PaymentQueryParser queryParser, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.queryParser = queryParser;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/api/transfers/internal")
    PaymentResponse internalTransfer(
            @AuthenticationPrincipal String customerId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId,
            @Valid @RequestBody InternalTransferRequest request
    ) {
        var customer = UUID.fromString(customerId);
        var corr = parseCorrelation(correlationId);
        return idempotencyService.run(customer, "INTERNAL_TRANSFER", idempotencyKey, request, PaymentResponse.class,
                () -> paymentService.createInternalTransfer(customer, request, authorization, corr));
    }

    @PostMapping("/api/payments/credit-card")
    PaymentResponse creditCardPayment(
            @AuthenticationPrincipal String customerId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId,
            @Valid @RequestBody CreditCardPaymentRequest request
    ) {
        var customer = UUID.fromString(customerId);
        var corr = parseCorrelation(correlationId);
        return idempotencyService.run(customer, "CREDIT_CARD_PAYMENT", idempotencyKey, request, PaymentResponse.class,
                () -> paymentService.createCreditCardPayment(customer, request, authorization, corr));
    }

    @PostMapping("/api/payments/external")
    PaymentResponse externalPayment(
            @AuthenticationPrincipal String customerId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId,
            @Valid @RequestBody ExternalPaymentRequest request
    ) {
        var customer = UUID.fromString(customerId);
        var corr = parseCorrelation(correlationId);
        return idempotencyService.run(customer, "EXTERNAL_PAYMENT", idempotencyKey, request, PaymentResponse.class,
                () -> paymentService.createExternalPayment(customer, request, authorization, corr));
    }

    @GetMapping("/api/payments")
    PageResponse<PaymentResponse> list(
            @AuthenticationPrincipal String customerId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return paymentService.list(UUID.fromString(customerId), queryParser.parse(type, status, from, to), page, size, sort);
    }

    @GetMapping("/api/payments/{paymentId}")
    PaymentResponse get(@AuthenticationPrincipal String customerId, @PathVariable UUID paymentId) {
        return paymentService.get(UUID.fromString(customerId), paymentId);
    }

    @GetMapping("/api/payments/{paymentId}/history")
    List<PaymentHistoryResponse> history(@AuthenticationPrincipal String customerId, @PathVariable UUID paymentId) {
        return paymentService.history(UUID.fromString(customerId), paymentId);
    }

    @PostMapping("/api/payments/{paymentId}/cancel")
    PaymentResponse cancel(
            @AuthenticationPrincipal String customerId,
            @PathVariable UUID paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId
    ) {
        var customer = UUID.fromString(customerId);
        return idempotencyService.run(customer, "CANCEL_PAYMENT", idempotencyKey, paymentId, PaymentResponse.class,
                () -> paymentService.cancel(customer, paymentId, parseCorrelation(correlationId)));
    }

    @PostMapping("/api/payments/internal/risk/{paymentId}/approve")
    PaymentResponse approve(@PathVariable UUID paymentId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                            @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId) {
        return paymentService.approveReviewed(paymentId, authorization, parseCorrelation(correlationId));
    }

    @PostMapping("/api/payments/internal/risk/{paymentId}/reject")
    PaymentResponse reject(@PathVariable UUID paymentId, @RequestHeader(value = Correlation.HEADER_NAME, required = false) String correlationId) {
        return paymentService.rejectReviewed(paymentId, parseCorrelation(correlationId));
    }

    private UUID parseCorrelation(String correlationId) {
        try {
            return correlationId == null || correlationId.isBlank() ? UUID.randomUUID() : UUID.fromString(correlationId);
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }
}
