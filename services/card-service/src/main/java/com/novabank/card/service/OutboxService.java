package com.novabank.card.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.card.domain.BankCard;
import com.novabank.card.domain.CardAction;
import com.novabank.card.domain.CardStatus;
import com.novabank.card.domain.OutboxEvent;
import com.novabank.card.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void add(BankCard card, CardAction action, CardStatus previousStatus, String correlationId, Instant occurredAt) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("_customerId", card.getCustomerId().toString());
        payload.put("cardId", card.getId().toString());
        payload.put("accountId", card.getAccountId().toString());
        payload.put("cardType", card.getCardType().name());
        payload.put("previousStatus", previousStatus == null ? null : previousStatus.name());
        payload.put("newStatus", card.getStatus().name());
        payload.put("action", action.name());
        payload.put("occurredAt", occurredAt.toString());

        var outbox = new OutboxEvent();
        outbox.setAggregateId(card.getId());
        outbox.setEventType(eventType(action));
        outbox.setCorrelationId(correlationId);
        try {
            outbox.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize card event payload", ex);
        }
        outboxRepository.save(outbox);
    }

    private String eventType(CardAction action) {
        return switch (action) {
            case CREATED -> "CardCreated";
            case ACTIVATED -> "CardActivated";
            case LOCKED -> "CardLocked";
            case UNLOCKED -> "CardUnlocked";
            case REPLACEMENT_REQUESTED -> "CardReplacementRequested";
            case CONTROLS_UPDATED -> "CardControlsUpdated";
            case EXPIRED -> "CardExpired";
            case CLOSED -> "CardClosed";
        };
    }
}
