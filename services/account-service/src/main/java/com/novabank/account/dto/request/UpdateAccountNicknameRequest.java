package com.novabank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountNicknameRequest(
        @NotBlank
        @Size(min = 2, max = 80)
        @Pattern(regexp = "^[A-Za-z0-9 .'-]+$", message = "Nickname contains unsupported characters")
        String nickname
) {
}
