package com.novabank.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 160) String usernameOrEmail,
        @NotBlank @Size(max = 100) String password
) {
}
