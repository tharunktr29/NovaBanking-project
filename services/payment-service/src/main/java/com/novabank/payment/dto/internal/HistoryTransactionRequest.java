package com.novabank.payment.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HistoryTransactionRequest(
        String sourceReference,
        UUID accountId,
        String direction,
        String type,
        String category,
        String description,
        BigDecimal amount,
        String currency,
        Instant postedAt
) {
}
