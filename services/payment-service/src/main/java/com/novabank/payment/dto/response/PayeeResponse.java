package com.novabank.payment.dto.response;

import com.novabank.payment.domain.ExternalAccountType;
import com.novabank.payment.domain.PayeeStatus;

import java.time.Instant;
import java.util.UUID;

public record PayeeResponse(
        UUID id,
        String payeeReference,
        String nickname,
        String bankName,
        ExternalAccountType accountType,
        String maskedAccountNumber,
        PayeeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
