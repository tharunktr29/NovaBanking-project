package com.novabank.transaction.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
