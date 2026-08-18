package com.novabank.card.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyCreditCardPaymentRequest(
        @NotNull UUID businessOperationId,
        @NotNull UUID cardId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) {
}
