package com.novabank.payment.dto.request;

import com.novabank.payment.domain.ExecutionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditCardPaymentRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationCardId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Size(max = 160) String memo,
        @NotNull ExecutionType executionType,
        Instant scheduledFor
) {
}
