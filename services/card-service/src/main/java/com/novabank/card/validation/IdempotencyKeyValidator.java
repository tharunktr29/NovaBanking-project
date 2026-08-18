package com.novabank.card.validation;

import com.novabank.card.exception.CardException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class IdempotencyKeyValidator {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,120}$");

    public String validate(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey) || !KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new CardException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "A valid Idempotency-Key header is required");
        }
        return idempotencyKey.trim();
    }
}
