package com.novabank.card.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateCardControlsRequest(
        @NotNull Boolean onlinePurchasesEnabled,
        @NotNull Boolean contactlessEnabled,
        @NotNull Boolean internationalPurchasesEnabled,
        @NotNull Boolean atmWithdrawalsEnabled,
        @NotNull @DecimalMin("0.00") BigDecimal dailyPurchaseLimit,
        @NotNull @DecimalMin("0.00") BigDecimal dailyAtmLimit,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) {
}
