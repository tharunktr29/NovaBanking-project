package com.novabank.payment.validation;

import com.novabank.payment.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class IdempotencyKeyValidator {
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._:-]{8,120}$");

    public String validate(String key) {
        if (key == null || key.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }
        var clean = key.trim();
        if (!KEY.matcher(clean).matches()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be 8-120 safe characters");
        }
        return clean;
    }
}
