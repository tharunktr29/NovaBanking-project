package com.novabank.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        UUID customerId,
        String username,
        String role
) {
}
