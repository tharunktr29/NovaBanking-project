package com.novabank.card.dto.response;

import com.novabank.card.domain.CardAction;
import com.novabank.card.domain.CardStatus;

import java.time.Instant;

public record CardHistoryResponse(
        CardAction action,
        CardStatus previousStatus,
        CardStatus newStatus,
        String reasonCode,
        Instant occurredAt
) {
}
