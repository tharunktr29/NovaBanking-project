package com.novabank.auth.web.dto;

import java.time.Instant;

public record LoginActivityResponse(
        Instant timestamp,
        boolean successful,
        String eventType,
        String maskedIpAddress,
        String deviceSummary
) {
}
