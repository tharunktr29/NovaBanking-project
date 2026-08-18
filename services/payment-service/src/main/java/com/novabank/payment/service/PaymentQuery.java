package com.novabank.payment.service;

import com.novabank.payment.domain.PaymentStatus;
import com.novabank.payment.domain.PaymentType;

import java.time.Instant;

public record PaymentQuery(
        PaymentType type,
        PaymentStatus status,
        Instant from,
        Instant to
) {
}
