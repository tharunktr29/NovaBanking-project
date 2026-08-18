package com.novabank.card.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    ABANDONED
}
