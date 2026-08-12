package com.novabank.account.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceResponse(
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
