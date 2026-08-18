package com.novabank.payment.dto.internal;

import java.math.BigDecimal;
import java.util.UUID;

public record PostLedgerTransactionRequest(
        UUID businessOperationId,
        String type,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String reference
) {
}
