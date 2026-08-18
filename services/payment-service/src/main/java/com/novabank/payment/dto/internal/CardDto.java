package com.novabank.payment.dto.internal;

import java.time.Instant;
import java.util.UUID;

public record CardDto(
        UUID cardId,
        UUID accountId,
        String cardType,
        String cardNetwork,
        String status,
        String cardholderName,
        String maskedCardNumber,
        String lastFour,
        Integer expirationMonth,
        Integer expirationYear,
        Instant activatedAt,
        Instant expiresAt,
        Object controls,
        Object credit,
        Object debitBalance,
        Object linkedAccount
) {
}
