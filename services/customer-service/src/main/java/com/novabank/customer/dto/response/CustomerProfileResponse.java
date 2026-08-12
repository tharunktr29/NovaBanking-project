package com.novabank.customer.dto.response;

import com.novabank.customer.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        UUID authUserId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        KycStatus kycStatus,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
