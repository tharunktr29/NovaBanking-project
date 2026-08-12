package com.novabank.customer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CustomerPreferenceResponse(
        UUID id,
        UUID customerId,
        boolean emailAlerts,
        boolean smsAlerts,
        boolean pushAlerts,
        boolean securityAlerts,
        boolean paymentAlerts,
        boolean lowBalanceAlerts,
        boolean paperlessStatements,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
