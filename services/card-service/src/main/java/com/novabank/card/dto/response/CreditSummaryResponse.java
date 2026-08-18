package com.novabank.card.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CreditSummaryResponse(
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        BigDecimal availableCredit,
        BigDecimal minimumPaymentDue,
        LocalDate paymentDueDate,
        String currency,
        Instant updatedAt
) {
}
