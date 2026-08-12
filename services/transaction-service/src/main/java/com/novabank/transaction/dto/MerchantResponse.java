package com.novabank.transaction.dto;

import java.util.UUID;

public record MerchantResponse(
        UUID merchantId,
        String name,
        String merchantCategoryCode,
        String city,
        String state,
        String country
) {
}
