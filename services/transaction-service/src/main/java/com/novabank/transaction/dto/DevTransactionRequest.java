package com.novabank.transaction.dto;

import com.novabank.transaction.domain.TransactionCategory;
import com.novabank.transaction.domain.TransactionDirection;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DevTransactionRequest(
        @NotNull UUID accountId,
        UUID merchantId,
        TransactionStatus status,
        @NotNull TransactionDirection direction,
        @NotNull TransactionType type,
        @NotNull TransactionCategory category,
        @NotBlank @Size(max = 240) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(min = 3, max = 3) String currency
) {
}
