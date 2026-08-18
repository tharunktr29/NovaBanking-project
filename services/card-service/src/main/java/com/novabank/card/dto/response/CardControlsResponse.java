package com.novabank.card.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardControlsResponse(
        UUID cardId,
        boolean onlinePurchasesEnabled,
        boolean contactlessEnabled,
        boolean internationalPurchasesEnabled,
        boolean atmWithdrawalsEnabled,
        BigDecimal dailyPurchaseLimit,
        BigDecimal dailyAtmLimit,
        String currency,
        Instant updatedAt
) {
}
