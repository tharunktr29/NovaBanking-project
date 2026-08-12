package com.novabank.transaction.dto;

import com.novabank.transaction.domain.TransactionCategory;
import com.novabank.transaction.domain.TransactionDirection;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID accountId,
        TransactionStatus status,
        TransactionDirection direction,
        TransactionType type,
        TransactionCategory category,
        String description,
        BigDecimal amount,
        String currency,
        MerchantResponse merchant,
        Instant authorizedAt,
        Instant postedAt
) {
}
