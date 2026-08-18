package com.novabank.payment.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    ABANDONED
}
