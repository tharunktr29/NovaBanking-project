package com.novabank.payment.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePayeeRequest(
        @Size(min = 2, max = 80) @Pattern(regexp = "^[A-Za-z0-9 .,'&-]+$") String nickname
) {
}
