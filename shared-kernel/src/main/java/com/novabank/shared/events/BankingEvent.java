package com.novabank.shared.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BankingEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant timestamp,
        String correlationId,
        UUID customerId,
        UUID aggregateId,
        Map<String, Object> payload
) {
    public static BankingEvent v1(
            String eventType,
            String correlationId,
            UUID customerId,
            UUID aggregateId,
            Map<String, Object> payload
    ) {
        return new BankingEvent(
                UUID.randomUUID(),
                eventType,
                1,
                Instant.now(),
                correlationId,
                customerId,
                aggregateId,
                payload == null ? Map.of() : Map.copyOf(payload)
        );
    }
}
