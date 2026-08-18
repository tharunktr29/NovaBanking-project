package com.novabank.payment.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplyCreditCardPaymentResponse(
        UUID id,
        UUID businessOperationId,
        UUID cardId,
        BigDecimal amount,
        String currency,
        Instant appliedAt
) {
}
