package com.novabank.payment.dto.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountDto(
        UUID id,
        UUID customerId,
        String accountType,
        String maskedAccountNumber,
        String nickname,
        String status,
        String currency,
        LocalDate openedDate,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        AccountBalanceDto balance
) {
}
