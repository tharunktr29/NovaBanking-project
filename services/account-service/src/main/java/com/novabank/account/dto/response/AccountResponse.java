package com.novabank.account.dto.response;

import com.novabank.account.domain.AccountStatus;
import com.novabank.account.domain.AccountType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        AccountType accountType,
        String maskedAccountNumber,
        String nickname,
        AccountStatus status,
        String currency,
        LocalDate openedDate,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        AccountBalanceResponse balance
) {
}
