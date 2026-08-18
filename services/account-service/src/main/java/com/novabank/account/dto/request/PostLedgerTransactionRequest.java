package com.novabank.account.dto.request;

import com.novabank.account.domain.LedgerTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PostLedgerTransactionRequest(
        @NotNull UUID businessOperationId,
        @NotNull LedgerTransactionType type,
        @NotBlank @Size(max = 120) String reference,
        @NotNull UUID sourceAccountId,
        UUID destinationAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) {
}
