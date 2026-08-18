package com.novabank.account.dto.response;

import com.novabank.account.domain.LedgerTransactionStatus;
import com.novabank.account.domain.LedgerTransactionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionResponse(
        UUID ledgerTransactionId,
        UUID businessOperationId,
        LedgerTransactionType type,
        LedgerTransactionStatus status,
        String reference,
        Instant postedAt,
        List<LedgerEntryResponse> entries
) {
}
