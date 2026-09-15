package com.novabank.payment.domain;

public enum PaymentStatus {
    SCHEDULED,
    PENDING,
    PROCESSING,
    UNDER_REVIEW,
    DECLINED,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSAL_PENDING,
    REVERSED
}
