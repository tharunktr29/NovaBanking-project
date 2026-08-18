package com.novabank.payment.service;

import com.novabank.payment.domain.PaymentStatus;
import com.novabank.payment.domain.PaymentType;
import com.novabank.payment.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PaymentQueryParser {
    public PaymentQuery parse(String type, String status, String from, String to) {
        return new PaymentQuery(parseType(type), parseStatus(status), parseInstant("from", from), parseInstant("to", to));
    }

    private PaymentType parseType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return PaymentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_TYPE", "Unsupported payment type filter");
        }
    }

    private PaymentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATUS", "Unsupported payment status filter");
        }
    }

    private Instant parseInstant(String name, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (Exception ex) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_DATE_FILTER", name + " must be an ISO-8601 instant");
        }
    }
}
