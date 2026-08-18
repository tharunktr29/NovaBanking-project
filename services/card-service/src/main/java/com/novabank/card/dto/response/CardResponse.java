package com.novabank.card.dto.response;

import com.novabank.card.domain.CardNetwork;
import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.CardType;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID cardId,
        UUID accountId,
        CardType cardType,
        CardNetwork cardNetwork,
        CardStatus status,
        String cardholderName,
        String maskedCardNumber,
        String lastFour,
        Integer expirationMonth,
        Integer expirationYear,
        Instant activatedAt,
        Instant expiresAt,
        CardControlsSummaryResponse controls,
        CreditSummaryResponse credit,
        DebitBalanceSummaryResponse debitBalance,
        LinkedAccountSummaryResponse linkedAccount
) {
}
