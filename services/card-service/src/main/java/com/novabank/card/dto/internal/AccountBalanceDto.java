package com.novabank.card.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceDto(
        UUID id,
        UUID accountId,
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        BigDecimal pendingDebitAmount,
        BigDecimal pendingCreditAmount,
        Instant asOf,
        Long version
) {
}
