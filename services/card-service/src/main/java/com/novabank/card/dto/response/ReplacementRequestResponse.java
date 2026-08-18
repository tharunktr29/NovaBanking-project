package com.novabank.card.dto.response;

import com.novabank.card.domain.ReplacementRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ReplacementRequestResponse(
        UUID cardId,
        String requestReference,
        ReplacementRequestStatus status,
        Instant requestedAt,
        String message
) {
}
