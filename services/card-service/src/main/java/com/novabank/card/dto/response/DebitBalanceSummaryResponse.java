package com.novabank.card.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record DebitBalanceSummaryResponse(
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        BigDecimal pendingDebitAmount,
        BigDecimal pendingCreditAmount,
        String currency,
        Instant asOf,
        boolean unavailable
) {
}
