package com.novabank.payment.dto.request;

import com.novabank.payment.domain.ExternalAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePayeeRequest(
        @NotBlank @Size(min = 2, max = 80) @Pattern(regexp = "^[A-Za-z0-9 .,'&-]+$") String nickname,
        @NotBlank @Size(min = 2, max = 120) @Pattern(regexp = "^[A-Za-z0-9 .,'&-]+$") String bankName,
        @NotNull ExternalAccountType accountType,
        @NotBlank @Pattern(regexp = "^\\*{4}\\s?\\d{4}$|^••••\\s?\\d{4}$") String maskedAccountNumber
) {
}
