package com.novabank.card.dto.response;

import java.math.BigDecimal;

public record CardControlsSummaryResponse(
        boolean onlinePurchasesEnabled,
        boolean contactlessEnabled,
        boolean internationalPurchasesEnabled,
        boolean atmWithdrawalsEnabled,
        BigDecimal dailyPurchaseLimit,
        BigDecimal dailyAtmLimit,
        String currency
) {
}
