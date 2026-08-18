package com.novabank.card.dto.request;

import com.novabank.card.domain.ReplacementReason;
import jakarta.validation.constraints.NotNull;

public record ReplacementRequestDto(
        @NotNull ReplacementReason reason
) {
}
