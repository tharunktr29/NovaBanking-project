package com.novabank.transaction.dto;

import com.novabank.transaction.domain.TransactionCategory;
import com.novabank.transaction.domain.TransactionDirection;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionQuery(
        UUID accountId,
        TransactionStatus status,
        TransactionDirection direction,
        TransactionType type,
        TransactionCategory category,
        String merchant,
        String search,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        int page,
        int size,
        String sort
) {
}
