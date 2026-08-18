package com.novabank.card.dto.response;

import java.util.UUID;

public record LinkedAccountSummaryResponse(
        UUID accountId,
        String nickname,
        String accountType,
        String status,
        boolean unavailable
) {
}
