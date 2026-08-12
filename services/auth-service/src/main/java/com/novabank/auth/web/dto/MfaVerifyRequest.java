package com.novabank.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "MFA code must be six digits") String code
) {
}
