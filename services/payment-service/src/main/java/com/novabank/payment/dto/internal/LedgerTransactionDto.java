package com.novabank.payment.dto.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionDto(
        UUID id,
        UUID businessOperationId,
        String type,
        String status,
        String reference,
        String correlationId,
        Instant createdAt,
        Instant postedAt,
        List<Object> entries
) {
}
