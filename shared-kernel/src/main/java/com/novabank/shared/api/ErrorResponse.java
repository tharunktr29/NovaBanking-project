package com.novabank.shared.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String correlationId,
        List<FieldViolation> fieldErrors
) {
    public static ErrorResponse of(
            int status,
            String code,
            String message,
            String path,
            String correlationId,
            List<FieldViolation> fieldErrors
    ) {
        return new ErrorResponse(
                Instant.now(),
                status,
                code,
                message,
                path,
                correlationId,
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors)
        );
    }
}
