package com.novabank.payment.dto.response;

import com.novabank.payment.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentReference,
        PaymentType paymentType,
        PaymentStatus status,
        UUID sourceAccountId,
        UUID destinationAccountId,
        UUID destinationCardId,
        UUID externalPayeeId,
        BigDecimal amount,
        String currency,
        String memo,
        ExecutionType executionType,
        Instant scheduledFor,
        Instant processingStartedAt,
        Instant completedAt,
        Instant failedAt,
        PaymentFailureCode failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
