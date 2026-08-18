package com.novabank.payment.dto.response;

import com.novabank.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String reasonCode,
        Instant occurredAt
) {
}
