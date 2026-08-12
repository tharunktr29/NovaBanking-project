package com.novabank.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank
        @Size(min = 12, max = 100)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include uppercase, lowercase, number, and symbol"
        )
        String newPassword
) {
}
