package com.novabank.card.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplyCreditCardPaymentResponse(
        UUID cardId,
        UUID businessOperationId,
        BigDecimal currentBalance,
        BigDecimal availableCredit,
        String currency,
        Instant appliedAt
) {
}
