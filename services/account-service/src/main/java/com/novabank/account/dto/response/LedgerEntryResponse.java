package com.novabank.account.dto.response;

import com.novabank.account.domain.LedgerEntryDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID accountId,
        LedgerEntryDirection direction,
        BigDecimal amount,
        String currency,
        BigDecimal balanceAfter,
        Instant createdAt
) {
}
