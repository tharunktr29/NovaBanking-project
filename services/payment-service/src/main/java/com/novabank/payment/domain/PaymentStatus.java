package com.novabank.payment.domain;

public enum PaymentStatus {
    SCHEDULED,
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSAL_PENDING,
    REVERSED
}
