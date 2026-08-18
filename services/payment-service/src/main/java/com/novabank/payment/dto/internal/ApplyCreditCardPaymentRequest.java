package com.novabank.payment.dto.internal;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyCreditCardPaymentRequest(
        UUID businessOperationId,
        UUID cardId,
        BigDecimal amount,
        String currency
) {
}
